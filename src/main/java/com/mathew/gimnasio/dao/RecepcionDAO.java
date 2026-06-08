/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;

/**
 * DAO DE RECEPCIÓN
 * Objeto de acceso a datos responsable de operar el flujo continuo del mostrador de atención.
 * Procesa en tiempo real el escáner de códigos QR para el control de accesos físicos,
 * monitorea la capacidad (aforo), y gestiona el flujo de caja e ingresos del día.
 */
public class RecepcionDAO {

    // ==========================================
    // 1. CARGAR DASHBOARD (AFORO Y CAJA) AISLADO POR EMPRESA
    // ==========================================

    /**
     * OBTENER DASHBOARD DE RECEPCIÓN (JSON)
     * Consolida las métricas clave del día actual en tiempo real, incluyendo la recaudación
     * en caja de membresías aprobadas, el aforo físico activo en las instalaciones, y el log
     * de las últimas entradas o salidas registradas.
     * Parametro idEmpresa: Identificador de la sucursal de operación.
     * Retorna: Una estructura JSON formateada con los KPIs diarios y la actividad reciente.
     */
    public String getDashboardRecepJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("{");

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. Ingresos en Caja (Aislado)
            double cajaHoy = 0.0;
            String sqlCaja = "SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE DATE(fecha_pago) = CURRENT_DATE AND id_empresa = ? AND estado = 'APROBADO'";
            try(PreparedStatement ps = conn.prepareStatement(sqlCaja)) {
                ps.setInt(1, idEmpresa);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) cajaHoy = rs.getDouble(1);
                }
            }

            // 2. Personas Entrenando (Aforo)
            int aforoHoy = 0;
            String sqlAforo = "SELECT COUNT(*) FROM asistencias a INNER JOIN clientes c ON a.id_cliente = c.id_cliente WHERE DATE(a.fecha_hora_ingreso) = CURRENT_DATE AND a.fecha_hora_salida IS NULL AND c.id_empresa = ?";
            try(PreparedStatement ps = conn.prepareStatement(sqlAforo)) {
                ps.setInt(1, idEmpresa);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) aforoHoy = rs.getInt(1);
                }
            }

            json.append("\"kpis\": {")
                    .append("\"cajaHoy\": ").append(cajaHoy).append(",")
                    .append("\"aforoHoy\": ").append(aforoHoy)
                    .append("},");

            // 3. Actividad Reciente FÍSICA
            json.append("\"actividadReciente\": [");
            String sqlActividad =
                    "SELECT u.usuario, a.fecha_hora_ingreso AS hora_movimiento, 'Entrada' AS tipo_movimiento " +
                            "FROM asistencias a " +
                            "INNER JOIN clientes c ON a.id_cliente = c.id_cliente " +
                            "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                            "WHERE DATE(a.fecha_hora_ingreso) = CURRENT_DATE AND u.id_empresa = ? " +
                            "UNION ALL " +
                            "SELECT u.usuario, a.fecha_hora_salida AS hora_movimiento, 'Salida' AS tipo_movimiento " +
                            "FROM asistencias a " +
                            "INNER JOIN clientes c ON a.id_cliente = c.id_cliente " +
                            "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                            "WHERE a.fecha_hora_salida IS NOT NULL AND DATE(a.fecha_hora_salida) = CURRENT_DATE AND u.id_empresa = ? " +
                            "ORDER BY hora_movimiento DESC LIMIT 5";

            try(PreparedStatement ps = conn.prepareStatement(sqlActividad)) {
                ps.setInt(1, idEmpresa);
                ps.setInt(2, idEmpresa);
                try(ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while(rs.next()) {
                        if(!first) json.append(",");

                        String horaMovimiento = rs.getString("hora_movimiento");
                        String tipoMovimiento = rs.getString("tipo_movimiento");

                        if(horaMovimiento != null && horaMovimiento.length() >= 16) {
                            horaMovimiento = horaMovimiento.substring(11, 16);
                        }

                        json.append("{")
                                .append("\"hora\": \"").append(horaMovimiento).append("\",")
                                .append("\"cliente\": \"").append(rs.getString("usuario")).append("\",")
                                .append("\"tipo\": \"").append(tipoMovimiento).append("\"")
                                .append("}");
                        first = false;
                    }
                }
            }
            json.append("]");

        } catch (Exception e) {
            System.out.println("Error en RecepcionDAO: " + e.getMessage());
            return "{\"kpis\": {\"cajaHoy\": 0, \"aforoHoy\": 0}, \"actividadReciente\": []}";
        }

        json.append("}");
        return json.toString();
    }

    // ==========================================
    // 2. PROCESAR EL ESCÁNER QR (ENTRADA / SALIDA)
    // ==========================================

    /**
     * PROCESAR LECTURA DE ACCESO QR
     * Gestiona la lógica de control de acceso físico a las instalaciones. Valida el estado activo
     * de la membresía, detecta si el evento corresponde a una entrada o una salida, y aplica la
     * restricción de negocio de un acceso único por día natural.
     * Parametro identificador: El código emitido por el escáner (puede contener prefijos del sistema).
     * Parametro idEmpresa: Identificador de la sucursal de acceso.
     * Retorna: JSON estructurado indicando el éxito (con mensaje de saludo/despedida) o un error detallado.
     */
    public String procesarAccesoQr(String identificador, int idEmpresa) {
        try (Connection conn = ConexionDB.getConnection()) {

            int idUsuario = -1;
            int idCliente = -1;
            boolean activo = false;
            String nombreUsuario = "";

            String paramLimpio = identificador.trim().toLowerCase();
            int idBuscado = -1;

            if (paramLimpio.startsWith("iron_")) {
                try { idBuscado = Integer.parseInt(paramLimpio.substring(5)); } catch (Exception e) {}
            } else {
                try { idBuscado = Integer.parseInt(paramLimpio); } catch (Exception e) {}
            }

            String sqlUser = "SELECT u.id_usuario, c.id_cliente, u.usuario, u.activo " +
                    "FROM usuarios u " +
                    "LEFT JOIN clientes c ON u.id_usuario = c.id_usuario " +
                    "LEFT JOIN entrenadores e ON u.id_usuario = e.id_usuario " +
                    "WHERE (LOWER(u.usuario) = ? OR LOWER(c.email) = ? OR LOWER(e.email) = ? OR u.id_usuario = ?) AND u.id_empresa = ?";

            try(PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setString(1, paramLimpio);
                ps.setString(2, paramLimpio);
                ps.setString(3, paramLimpio);
                ps.setInt(4, idBuscado);
                ps.setInt(5, idEmpresa);

                ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    idUsuario = rs.getInt("id_usuario");
                    idCliente = rs.getInt("id_cliente");
                    nombreUsuario = rs.getString("usuario");
                    activo = rs.getBoolean("activo");
                }
            }

            if (idUsuario == -1) return "{\"status\":\"error\", \"mensaje\":\"Usuario no encontrado en esta sucursal.\"}";
            if (!activo) return "{\"status\":\"error\", \"mensaje\":\"El usuario está inactivo. Verifique sus pagos.\"}";
            if (idCliente <= 0) return "{\"status\":\"error\", \"mensaje\":\"El usuario existe pero no está registrado como cliente.\"}";

            int idAsistencia = -1;
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_cliente = ? AND DATE(fecha_hora_ingreso) = CURRENT_DATE AND fecha_hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, idCliente);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) idAsistencia = rs.getInt("id_asistencia");
            }

            if (idAsistencia != -1) {
                // MARCAR SALIDA
                String sqlOut = "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP WHERE id_asistencia = ?";
                try(PreparedStatement ps = conn.prepareStatement(sqlOut)) {
                    ps.setInt(1, idAsistencia);
                    ps.executeUpdate();
                }

                java.time.ZonedDateTime ahoraEcuador = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Guayaquil"));
                String horaFmt = ahoraEcuador.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

                return "{\"status\":\"ok\", \"tipo\":\"Salida\", \"mensaje\":\"¡Hasta pronto, " + nombreUsuario + "! Salida a las " + horaFmt + "\"}";
            } else {
                // REGLA DE NEGOCIO: 1 ACCESO POR DÍA
                String sqlValidacionDia = "SELECT COUNT(*) FROM asistencias WHERE id_cliente = ? AND DATE(fecha_hora_ingreso) = CURRENT_DATE";

                try(PreparedStatement psValidacion = conn.prepareStatement(sqlValidacionDia)) {
                    psValidacion.setInt(1, idCliente);
                    ResultSet rsValidacion = psValidacion.executeQuery();

                    if (rsValidacion.next() && rsValidacion.getInt(1) > 0) {
                        return "{\"status\":\"error\", \"mensaje\":\"Acceso Denegado: Ya utilizaste tu acceso del día de hoy. ¡Vuelve mañana!\"}";
                    }
                }

                // MARCAR ENTRADA
                String codigoUnico = identificador.trim() + "_" + System.currentTimeMillis();

                String sqlIn = "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso, dispositivo_qr, codigo_validado) VALUES (?, CURRENT_TIMESTAMP, 'Escáner Recepción', ?)";
                try(PreparedStatement ps = conn.prepareStatement(sqlIn)) {
                    ps.setInt(1, idCliente);
                    ps.setString(2, codigoUnico);
                    ps.executeUpdate();
                }

                java.time.ZonedDateTime ahoraEcuador = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Guayaquil"));
                String horaFmt = ahoraEcuador.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

                return "{\"status\":\"ok\", \"tipo\":\"Entrada\", \"mensaje\":\"¡Bienvenido a entrenar, " + nombreUsuario + "! Entrada a las " + horaFmt + "\"}";
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'").replace("\n", " ") : "Error desconocido";
            return "{\"status\":\"error\", \"mensaje\":\"Error BD: " + errorMsg + "\"}";
        }
    }

    // ==========================================
    // 3. OBTENER DIRECTORIO DE SOCIOS (CON ESTADO EN VIVO)
    // ==========================================

    /**
     * OBTENER DIRECTORIO DE SOCIOS
     * Extrae el catálogo completo de usuarios con rol de cliente inscritos en la sucursal actual.
     * Incorpora una subconsulta anidada para determinar en tiempo real si el socio se encuentra
     * entrenando físicamente en las instalaciones.
     * Parametro idEmpresa: Identificador de la sucursal a la que pertenecen los socios.
     * Retorna: Una cadena JSON que representa el listado de clientes y su estado actual de aforo.
     */
    public String obtenerSociosRecepcionJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT u.id_usuario, u.usuario, u.nombre, u.apellido, u.activo, " +
                "c.email, c.telefono, " +
                "(SELECT COUNT(*) FROM asistencias a WHERE a.id_cliente = c.id_cliente AND DATE(a.fecha_hora_ingreso) = CURRENT_DATE AND a.fecha_hora_salida IS NULL) as esta_entrenando " +
                "FROM usuarios u " +
                "INNER JOIN clientes c ON u.id_usuario = c.id_usuario " +
                "WHERE u.id_rol = 4 AND u.id_empresa = ? " +
                "ORDER BY u.id_usuario DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpresa);

            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id_usuario")).append(",")
                            .append("\"usuario\":\"").append(rs.getString("usuario")).append("\",")
                            .append("\"nombre\":\"").append(rs.getString("nombre") != null ? rs.getString("nombre") : "").append("\",")
                            .append("\"apellido\":\"").append(rs.getString("apellido") != null ? rs.getString("apellido") : "").append("\",")
                            .append("\"activo\":").append(rs.getBoolean("activo")).append(",")
                            .append("\"email\":\"").append(rs.getString("email") != null ? rs.getString("email") : "").append("\",")
                            .append("\"telefono\":\"").append(rs.getString("telefono") != null ? rs.getString("telefono") : "").append("\",")
                            .append("\"esta_entrenando\":").append(rs.getInt("esta_entrenando"))
                            .append("}");
                    first = false;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    // ==========================================
    // 4. OBTENER HISTORIAL DE CAJA (PAGOS)
    // ==========================================

    /**
     * OBTENER HISTORIAL DE PAGOS APROBADOS
     * Consulta y formatea un registro de las últimas 50 transacciones económicas exitosas (aprobadas)
     * procesadas por la sucursal para la auditoría y control de caja por parte de recepción.
     * Parametro idEmpresa: Identificador de la sucursal recaudadora.
     * Retorna: JSON Array con el historial de pagos.
     */
    public String obtenerHistorialPagosJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT p.id_pago, u.usuario, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.id_membresia " +
                "FROM pagos p " +
                "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "WHERE p.id_empresa = ? AND p.estado = 'APROBADO' " +
                "ORDER BY p.fecha_pago DESC LIMIT 50";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpresa);

            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while(rs.next()) {
                    if(!first) json.append(",");
                    String fechaLimpia = rs.getString("fecha_pago");
                    if(fechaLimpia != null && fechaLimpia.length() > 19) fechaLimpia = fechaLimpia.substring(0, 19);

                    json.append("{")
                            .append("\"id_pago\":").append(rs.getInt("id_pago")).append(",")
                            .append("\"socio\":\"").append(rs.getString("usuario")).append("\",")
                            .append("\"monto\":").append(rs.getDouble("monto_pagado")).append(",")
                            .append("\"fecha\":\"").append(fechaLimpia).append("\",")
                            .append("\"metodo\":\"").append(rs.getString("metodo_pago")).append("\",")
                            .append("\"id_plan\":").append(rs.getInt("id_membresia"))
                            .append("}");
                    first = false;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    // ==========================================
    // 5. REGISTRAR UN NUEVO PAGO (CON ESTADO APROBADO AUTOMÁTICO EN CAJA)
    // ==========================================

    /**
     * REGISTRAR PAGO PRESENCIAL (MOSTRADOR)
     * Ejecuta una transacción de base de datos para ingresar un pago recibido de forma directa
     * en las instalaciones. Al ser verificado físicamente por el personal, el estado del pago
     * se establece automáticamente en 'APROBADO' y se actualiza la vigencia de la membresía del cliente.
     * Parametro idUsuarioEnviado: Identificador del cliente.
     * Parametro idPlan: Identificador de la membresía contratada.
     * Parametro monto: Valor económico recibido.
     * Parametro metodo: Modalidad de transacción (Efectivo, Tarjeta, etc.).
     * Parametro idEmpresa: Identificador de la sucursal de cobro.
     * Retorna: Verdadero si ambas inserciones (pago y actualización de perfil) concluyen satisfactoriamente.
     */
    public boolean registrarPago(int idUsuarioEnviado, int idPlan, double monto, String metodo, int idEmpresa) {
        // Los pagos recibidos físicamente en recepción nacen como APROBADOS
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago, id_empresa, estado) " +
                "SELECT id_cliente, ?, ?, ?, CURRENT_TIMESTAMP, ?, 'APROBADO' " +
                "FROM clientes WHERE id_usuario = ?";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPlan);
                ps.setDouble(2, monto);
                ps.setString(3, metodo);
                ps.setInt(4, idEmpresa);
                ps.setInt(5, idUsuarioEnviado);

                int filasAfectadas = ps.executeUpdate();
                if (filasAfectadas > 0) {
                    // Al ser un pago en mostrador, se le activa la membresía automáticamente extendiéndola por 1 mes
                    String sqlCliente = "UPDATE clientes SET id_membresia = ?, fecha_vencimiento = CURRENT_DATE + INTERVAL '1 month', cancelado = FALSE " +
                            "WHERE id_usuario = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(sqlCliente)) {
                        ps2.setInt(1, idPlan);
                        ps2.setInt(2, idUsuarioEnviado);
                        ps2.executeUpdate();
                    }
                    conn.commit();
                    return true;
                }
                conn.rollback();
                return false;
            }
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }

    // ==========================================
    // 6. OBTENER PAGOS PENDIENTES DE REVISIÓN
    // ==========================================

    /**
     * OBTENER LISTADO DE PAGOS PENDIENTES
     * Selecciona todos los comprobantes de depósito o transferencias enviadas por los clientes
     * a través del portal en línea que aguardan por validación humana, ordenándolos cronológicamente.
     * Parametro idEmpresa: Identificador de la sucursal correspondiente.
     * Retorna: JSON Array detallado con la información y referencias visuales de los pagos a revisar.
     */
    public String obtenerPagosPendientesJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT p.id_pago, u.usuario AS nombre_cliente, p.monto_pagado, " +
                "to_char(p.fecha_pago, 'YYYY-MM-DD HH24:MI') as fecha_formateada, " +
                "p.id_membresia, p.referencia_comprobante, p.foto_comprobante, p.motivo, p.estado " +
                "FROM pagos p " +
                "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "WHERE p.id_empresa = ? " +
                "ORDER BY " +
                "  CASE WHEN p.estado = 'PENDIENTE' THEN 1 ELSE 2 END, " +
                "  p.fecha_pago DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                            .append("\"id_pago\":").append(rs.getInt("id_pago")).append(",")
                            .append("\"nombre_cliente\":\"").append(rs.getString("nombre_cliente")).append("\",")
                            .append("\"monto_pagado\":").append(rs.getDouble("monto_pagado")).append(",")
                            .append("\"fecha_pago\":\"").append(rs.getString("fecha_formateada")).append("\",")
                            .append("\"id_membresia\":").append(rs.getInt("id_membresia")).append(",")
                            .append("\"estado\":\"").append(rs.getString("estado")).append("\",")
                            .append("\"numero_referencia\":\"").append(rs.getString("referencia_comprobante") != null ? rs.getString("referencia_comprobante") : "").append("\",")
                            .append("\"motivo\":\"").append(rs.getString("motivo") != null ? rs.getString("motivo") : "Renovación").append("\",")
                            .append("\"foto_comprobante\":\"").append(rs.getString("foto_comprobante") != null ? rs.getString("foto_comprobante") : "").append("\"")
                            .append("}");
                    first = false;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    // ==========================================
    // 7. VERIFICAR PAGO (APROBAR Y ACTIVAR MEMBRESÍA O RECHAZAR)
    // ==========================================

    /**
     * VERIFICAR COMPROBANTE DE PAGO
     * Consolida la decisión del auditor de recepción sobre un comprobante. Si el pago es marcado
     * como 'APROBADO', se inicia una transacción que actualiza el estado económico y renueva
     * automáticamente el acceso del cliente a las instalaciones por un periodo de un mes.
     * Parametro idPago: Identificador del comprobante revisado.
     * Parametro estado: Dictamen final ('APROBADO' o 'RECHAZADO').
     * Parametro idMembresia: Identificador del plan a activar en caso de aprobación.
     * Retorna: Verdadero tras la culminación exitosa del proceso de base de datos.
     */
    public boolean verificarPago(int idPago, String estado, int idMembresia) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            String sqlPago = "UPDATE pagos SET estado = ? WHERE id_pago = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(sqlPago)) {
                ps1.setString(1, estado);
                ps1.setInt(2, idPago);
                ps1.executeUpdate();
            }

            if ("APROBADO".equals(estado)) {
                String sqlCliente = "UPDATE clientes SET id_membresia = ?, fecha_vencimiento = CURRENT_DATE + INTERVAL '1 month', cancelado = FALSE " +
                        "WHERE id_cliente = (SELECT id_cliente FROM pagos WHERE id_pago = ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(sqlCliente)) {
                    ps2.setInt(1, idMembresia);
                    ps2.setInt(2, idPago);
                    ps2.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }

    // ==========================================
    // MÉTODOS DE VALIDACIÓN E INTEGRIDAD (NUEVOS)
    // ==========================================

    /**
     * VALIDAR PERTENENCIA A SUCURSAL
     * Verifica que un cliente específico se encuentre registrado legítimamente dentro de la sucursal
     * donde intenta operar o de donde recibe el cobro.
     * Parametro idUsuario: Identificador de la cuenta del cliente.
     * Parametro idEmpresa: Identificador de la sucursal de cotejo.
     * Retorna: Verdadero si la asociación es comprobable.
     */
    public boolean existeUsuarioEnEmpresa(int idUsuario, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM clientes c INNER JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE u.id_usuario = ? AND u.id_empresa = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * VALIDAR ELEGIBILIDAD DEL PLAN
     * Confirma que una membresía solicitada para pago corresponda efectivamente a un plan
     * ofertado por la sucursal actual, previniendo alteraciones o inconsistencias entre sedes.
     * Parametro idPlan: Identificador de la membresía comercializada.
     * Parametro idEmpresa: Identificador de la empresa vendedora.
     * Retorna: Verdadero si la integridad relacional del plan es válida.
     */
    public boolean existePlanEnEmpresa(int idPlan, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM membresias WHERE id_membresia = ? AND id_empresa = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * VALIDAR ESTADO DEL COMPROBANTE PENDIENTE
     * Salvaguarda empleada durante la validación de pagos para asegurar que el sistema
     * no intente re-procesar (doble aprobación o rechazo) un pago que ya había sido dictaminado.
     * Parametro idPago: Identificador del pago a validar.
     * Parametro idEmpresa: Identificador de la sucursal por motivos de auditoría cruzada.
     * Retorna: Verdadero si el comprobante aún se encuentra en estado 'PENDIENTE'.
     */
    public boolean esPagoPendienteValido(int idPago, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM pagos WHERE id_pago = ? AND id_empresa = ? AND estado = 'PENDIENTE'";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPago);
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}