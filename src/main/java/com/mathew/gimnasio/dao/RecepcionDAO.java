package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;

/**
 * DATA ACCESS OBJECT (DAO) DE RECEPCIÓN
 * Actúa como el motor transaccional del "Front Desk".
 * Gestiona el Punto de Venta (POS), la telemetría en vivo del aforo físico
 * y la validación de control de acceso mediante códigos QR.
 */
public class RecepcionDAO {

    /**
     * OBTENER TELEMETRÍA DEL DASHBOARD (AFORO Y CAJA)
     * Construye manualmente un JSON anidado. Consolida el flujo de caja diario,
     * la cantidad de personas actualmente dentro de las instalaciones y un
     * feed de actividad en tiempo real.
     */
    // ==========================================
    // 1. CARGAR DASHBOARD (AFORO Y CAJA) AISLADO POR EMPRESA
    // ==========================================
    public String getDashboardRecepJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("{");

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. Ingresos en Caja (Aislado)
            double cajaHoy = 0.0;
            String sqlCaja = "SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE DATE(fecha_pago) = CURRENT_DATE AND id_empresa = ?";
            try(PreparedStatement ps = conn.prepareStatement(sqlCaja)) {
                ps.setInt(1, idEmpresa);
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) cajaHoy = rs.getDouble(1);
                }
            }

            // 2. Personas Entrenando (Aforo actual en vivo: Aislado por la empresa del cliente)
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

            // 3. Actividad Reciente FÍSICA (Aislada por empresa en ambas subconsultas)
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
                ps.setInt(2, idEmpresa); // Se inyecta dos veces por el UNION ALL
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

    /**
     * NÚCLEO DE CONTROL DE ACCESO (QR SCANNER) AISLADO
     */
    // ==========================================
    // 2. PROCESAR EL ESCÁNER QR (ENTRADA / SALIDA)
    // ==========================================
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

            // Búsqueda flexible pero SECUTIRIZADA POR EMPRESA
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
                ps.setInt(5, idEmpresa); // Filtro multi-tenant

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
                // ==========================================
                // CASO A: MARCAR SALIDA (SE QUEDA IGUAL)
                // ==========================================
                String sqlOut = "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP WHERE id_asistencia = ?";
                try(PreparedStatement ps = conn.prepareStatement(sqlOut)) {
                    ps.setInt(1, idAsistencia);
                    ps.executeUpdate();
                }

                java.time.ZonedDateTime ahoraEcuador = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Guayaquil"));
                String horaFmt = ahoraEcuador.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

                return "{\"status\":\"ok\", \"tipo\":\"Salida\", \"mensaje\":\"¡Hasta pronto, " + nombreUsuario + "! Salida a las " + horaFmt + "\"}";
            } else {
                // ==========================================
                // REGLA DE NEGOCIO: 1 ACCESO (ENTRADA/SALIDA) POR DÍA
                // ==========================================
                String sqlValidacionDia = "SELECT COUNT(*) FROM asistencias WHERE id_cliente = ? AND DATE(fecha_hora_ingreso) = CURRENT_DATE";

                try(PreparedStatement psValidacion = conn.prepareStatement(sqlValidacionDia)) {
                    psValidacion.setInt(1, idCliente);
                    ResultSet rsValidacion = psValidacion.executeQuery();

                    if (rsValidacion.next() && rsValidacion.getInt(1) > 0) {
                        // Si el conteo es mayor a 0, ya consumió su pase diario. Se rechaza la entrada.
                        return "{\"status\":\"error\", \"mensaje\":\"Acceso Denegado: Ya utilizaste tu acceso del día de hoy. ¡Vuelve mañana!\"}";
                    }
                }

                // ==========================================
                // CASO B: MARCAR ENTRADA (SI PASÓ LA VALIDACIÓN)
                // ==========================================
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
            String errorMsg = e.getMessage();
            if(errorMsg != null) {
                errorMsg = errorMsg.replace("\"", "'").replace("\n", " ");
            } else {
                errorMsg = "Error desconocido";
            }
            System.out.println("Error procesando QR: " + errorMsg);
            return "{\"status\":\"error\", \"mensaje\":\"Error BD: " + errorMsg + "\"}";
        }
    }

    /**
     * OBTENER DIRECTORIO EN VIVO DE SOCIOS AISLADO
     */
    // ==========================================
    // 3. OBTENER DIRECTORIO DE SOCIOS (CON ESTADO EN VIVO)
    // ==========================================
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

            ps.setInt(1, idEmpresa); // Filtramos por empresa

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

    /**
     * OBTENER HISTORIAL DE VENTAS (CAJA CHICA) AISLADO
     */
    // ==========================================
    // 4. OBTENER HISTORIAL DE CAJA (PAGOS)
    // ==========================================
    public String obtenerHistorialPagosJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");

        String sql = "SELECT p.id_pago, u.usuario, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.id_membresia " +
                "FROM pagos p " +
                "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "WHERE p.id_empresa = ? " +
                "ORDER BY p.fecha_pago DESC LIMIT 50";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpresa); // Filtramos los cobros por gimnasio

            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while(rs.next()) {
                    if(!first) json.append(",");

                    String fechaLimpia = rs.getString("fecha_pago");
                    if(fechaLimpia != null && fechaLimpia.length() > 19) {
                        fechaLimpia = fechaLimpia.substring(0, 19);
                    }

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
        } catch (Exception e) {
            System.out.println("Error Historial Pagos: " + e.getMessage());
        }
        json.append("]");
        return json.toString();
    }

    /**
     * PROCESAMIENTO DE PUNTO DE VENTA (POS) AISLADO
     */
    // ==========================================
    // 5. REGISTRAR UN NUEVO PAGO
    // ==========================================
    public boolean registrarPago(int idUsuarioEnviado, int idPlan, double monto, String metodo, int idEmpresa) {
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago, id_empresa) " +
                "SELECT id_cliente, ?, ?, ?, CURRENT_TIMESTAMP, ? " +
                "FROM clientes WHERE id_usuario = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPlan);
            ps.setDouble(2, monto);
            ps.setString(3, metodo);
            ps.setInt(4, idEmpresa); // Registramos a qué empresa va el dinero
            ps.setInt(5, idUsuarioEnviado);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            System.out.println("Error Registrando Pago en BD: " + e.getMessage());
            return false;
        }
    }
    // ==========================================
    // 6. OBTENER PAGOS PENDIENTES DE REVISIÓN
    // ==========================================
    // ==========================================
    // OBTENER TODOS LOS PAGOS (PENDIENTES, APROBADOS Y RECHAZADOS)
    // ==========================================
    public String obtenerPagosPendientesJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");

        // Eliminamos "AND p.estado = 'PENDIENTE'" y añadimos un ORDER BY inteligente
        String sql = "SELECT p.id_pago, u.usuario AS nombre_cliente, p.monto_pagado, " +
                "to_char(p.fecha_pago, 'YYYY-MM-DD HH24:MI') as fecha_formateada, " +
                "p.id_membresia, p.referencia_comprobante, p.foto_comprobante, p.motivo, p.estado " +
                "FROM pagos p " +
                "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "WHERE p.id_empresa = ? " +
                "ORDER BY " +
                "  CASE WHEN p.estado = 'PENDIENTE' THEN 1 ELSE 2 END, " +
                "  p.fecha_pago DESC"; // Los pendientes salen arriba, luego el resto ordenados por fecha

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
                            .append("\"numero_referencia\":\"").append(rs.getString("referencia_comprobante")).append("\",")
                            .append("\"motivo\":\"").append(rs.getString("motivo") != null ? rs.getString("motivo") : "Renovación").append("\",")
                            .append("\"foto_comprobante\":\"").append(rs.getString("foto_comprobante")).append("\"")
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
    public boolean verificarPago(int idPago, String estado, int idMembresia) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciamos transacción

            // 1. Actualizar el estado del pago
            String sqlPago = "UPDATE pagos SET estado = ? WHERE id_pago = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(sqlPago)) {
                ps1.setString(1, estado);
                ps1.setInt(2, idPago);
                ps1.executeUpdate();
            }

            // 2. Si es APROBADO, actualizar la membresía del cliente
            if ("APROBADO".equals(estado)) {
                String sqlCliente = "UPDATE clientes SET id_membresia = ?, fecha_vencimiento = CURRENT_DATE + INTERVAL '1 month', cancelado = FALSE " +
                        "WHERE id_cliente = (SELECT id_cliente FROM pagos WHERE id_pago = ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(sqlCliente)) {
                    ps2.setInt(1, idMembresia);
                    ps2.setInt(2, idPago);
                    ps2.executeUpdate();
                }
            }

            conn.commit(); // Confirmamos los cambios
            return true;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }
}