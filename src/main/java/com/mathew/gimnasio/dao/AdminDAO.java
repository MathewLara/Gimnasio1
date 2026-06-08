/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.DashboardDTO;
import com.mathew.gimnasio.modelos.AccesoDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DE ADMINISTRADOR
 * Componente de acceso a datos encargado de gestionar las operaciones críticas
 * del panel de administración. Procesa las consultas de telemetría del dashboard,
 * la recuperación del historial financiero, el registro de pagos y la administración
 * integral del catálogo de membresías, asegurando conexiones seguras a la base de datos.
 */
public class AdminDAO {

    // ==========================================
    // 1. OBTENER ESTADÍSTICAS
    // ==========================================

    /**
     * OBTENER MÉTRICAS DEL DASHBOARD
     * Ejecuta múltiples consultas a la base de datos para compilar los indicadores
     * clave de rendimiento (KPIs), como total de cuentas, ingresos, membresías vencidas
     * y el registro de los últimos accesos al sistema.
     * Parametro idEmpresa: Identificador numérico de la sucursal a consultar.
     * Retorna: Un objeto DashboardDTO poblado con todas las estadísticas solicitadas.
     */
    public DashboardDTO obtenerEstadisticas(int idEmpresa) {
        DashboardDTO dash = new DashboardDTO();
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes WHERE id_empresa = ?")) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dash.setTotalCuentas(rs.getInt(1));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE id_empresa = ?")) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dash.setIngresos(rs.getDouble(1));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM entrenadores WHERE id_empresa = ?")) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dash.setTotalEntrenadores(rs.getInt(1));
                }
            }

            String sqlVencidas = "SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento < CURRENT_DATE AND id_empresa = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlVencidas)) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dash.setMembresiasVencidas(rs.getInt(1));
                }
            }

            List<AccesoDTO> accesos = new ArrayList<>();
            String sqlAccesos = "SELECT u.usuario, u.id_rol, TO_CHAR(a.fecha_hora_log - INTERVAL '5 hours', 'YYYY-MM-DD HH24:MI:SS') AS fecha_ec, a.direccion_ip, a.exitoso " +
                    "FROM logs_acceso a INNER JOIN usuarios u ON a.id_usuario = u.id_usuario " +
                    "WHERE u.id_empresa = ? ORDER BY a.fecha_hora_log DESC LIMIT 5";

            try (PreparedStatement ps = conn.prepareStatement(sqlAccesos)) {
                ps.setInt(1, idEmpresa);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AccesoDTO acc = new AccesoDTO();
                        acc.setUsuario(rs.getString("usuario"));
                        acc.setRol(rs.getInt("id_rol") == 1 ? "Admin" : "Cliente");
                        acc.setHoraIngreso(rs.getString("fecha_ec") != null ? rs.getString("fecha_ec") : "---");
                        acc.setIp(rs.getString("direccion_ip") != null ? rs.getString("direccion_ip") : "Desconocida");
                        acc.setEstado(rs.getBoolean("exitoso") ? "Exitoso" : "Fallido");
                        accesos.add(acc);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            dash.setUltimosAccesos(accesos);

        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if (conn != null) conn.close(); } catch (Exception e) {} }
        return dash;
    }

    // ==========================================
    // 2. HISTORIAL DE PAGOS
    // ==========================================

    /**
     * OBTENER HISTORIAL DE PAGOS (JSON)
     * Realiza una consulta relacional para extraer el registro histórico de todos los cobros
     * procesados en la sucursal, formateando la salida directamente como una cadena JSON.
     * Parametro idEmpresa: Identificador numérico de la sucursal.
     * Retorna: Un String con formato JSON Array que contiene el historial de transacciones.
     */
    public String obtenerHistorialPagosJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT p.id_pago, u.usuario, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.id_membresia " +
                "FROM pagos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "WHERE p.id_empresa = ? ORDER BY p.fecha_pago DESC";

        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                            .append("\"id_pago\":").append(rs.getInt("id_pago")).append(",")
                            .append("\"socio\":\"").append(rs.getString("usuario")).append("\",")
                            .append("\"monto\":").append(rs.getDouble("monto_pagado")).append(",")
                            .append("\"fecha\":\"").append(rs.getString("fecha_pago")).append("\",")
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
    // 3. REGISTRAR PAGO
    // ==========================================

    /**
     * REGISTRAR PAGO MANUAL
     * Inserta un nuevo registro transaccional en la tabla de pagos, relacionándolo
     * directamente con el perfil del cliente mediante una subconsulta segura.
     * Parametro idUsuario: Identificador del cliente que realiza el pago.
     * Parametro idPlan: Identificador de la membresía adquirida.
     * Parametro monto: Cantidad monetaria cancelada.
     * Parametro metodo: Forma de pago utilizada (Efectivo, Tarjeta, etc.).
     * Parametro idEmpresa: Identificador de la sucursal recaudadora.
     * Retorna: Verdadero si la inserción fue exitosa, Falso en caso de error o excepciones.
     */
    public boolean registrarPago(int idUsuario, int idPlan, double monto, String metodo, int idEmpresa) {
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago, id_empresa) " +
                "SELECT id_cliente, ?, ?, ?, CURRENT_TIMESTAMP, ? " +
                "FROM clientes WHERE id_usuario = ?";

        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            ps.setDouble(2, monto);
            ps.setString(3, metodo);
            ps.setInt(4, idEmpresa);
            ps.setInt(5, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ==========================================
    // 4. GESTIÓN DE PLANES / MEMBRESÍAS
    // ==========================================

    /**
     * OBTENER CATÁLOGO DE PLANES (JSON)
     * Recupera todos los planes de membresía, activos o inactivos, registrados para una empresa
     * y los serializa manualmente en una cadena JSON para su uso en el panel administrativo.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: JSON Array en formato String con los datos de las membresías.
     */
    public String obtenerPlanesJSON(int idEmpresa) {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT id_membresia, nombre, precio, descripcion, activo FROM membresias WHERE id_empresa = ? ORDER BY id_membresia ASC";

        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                            .append("\"id\":").append(rs.getInt("id_membresia")).append(",")
                            .append("\"nombre\":\"").append(rs.getString("nombre")).append("\",")
                            .append("\"precio\":").append(rs.getDouble("precio")).append(",")
                            .append("\"descripcion\":\"").append(rs.getString("descripcion") != null ? rs.getString("descripcion").replace("\n", " ").replace("\r", "") : "").append("\",")
                            .append("\"activo\":").append(rs.getBoolean("activo"))
                            .append("}");
                    first = false;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    /**
     * GUARDAR NUEVO PLAN
     * Inserta una nueva oferta de membresía en el catálogo de la sucursal,
     * estableciéndola como activa por defecto.
     * Parametro nombre: Denominación comercial del plan.
     * Parametro precio: Costo asociado a la membresía.
     * Parametro descripcion: Detalles o beneficios incluidos en el plan.
     * Parametro idEmpresa: Identificador de la sucursal propietaria.
     * Retorna: Verdadero si se almacenó correctamente.
     */
    public boolean guardarPlan(String nombre, double precio, String descripcion, int idEmpresa) {
        String sql = "INSERT INTO membresias (nombre, precio, descripcion, id_empresa, activo) VALUES (?, ?, ?, ?, TRUE)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setDouble(2, precio);
            ps.setString(3, descripcion.trim());
            ps.setInt(4, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * EDITAR PLAN EXISTENTE
     * Actualiza la información descriptiva y de costos de una membresía previamente registrada.
     * Parametro id: Identificador de la membresía a modificar.
     * Parametro nombre: Nuevo nombre comercial.
     * Parametro precio: Nuevo costo.
     * Parametro descripcion: Nueva descripción de beneficios.
     * Parametro idEmpresa: Identificador de la sucursal por motivos de seguridad.
     * Retorna: Verdadero si la actualización afectó al menos a una fila.
     */
    public boolean editarPlan(int id, String nombre, double precio, String descripcion, int idEmpresa) {
        String sql = "UPDATE membresias SET nombre=?, precio=?, descripcion=? WHERE id_membresia=? AND id_empresa=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setDouble(2, precio);
            ps.setString(3, descripcion.trim());
            ps.setInt(4, id);
            ps.setInt(5, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * CAMBIAR ESTADO DE PLAN
     * Ejecuta un borrado lógico (soft-delete) o reactivación de un plan de membresía
     * modificando su indicador de disponibilidad en la base de datos.
     * Parametro id: Identificador del plan.
     * Parametro estado: Booleano que define si el plan será visible o no.
     * Retorna: Verdadero si el cambio de estado fue exitoso.
     */
    public boolean cambiarEstadoPlan(int id, boolean estado) {
        String sql = "UPDATE membresias SET activo=? WHERE id_membresia=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ==========================================
    // 5. OBTENER PLANES ACTIVOS PARA EL INDEX PÚBLICO
    // ==========================================

    /**
     * OBTENER PLANES ACTIVOS (PÚBLICO)
     * Recupera exclusivamente las membresías habilitadas (activas) para renderizarlas
     * en la vista pública de aterrizaje (Landing Page) para los clientes finales.
     * Retorna: Cadena JSON con la lista de planes disponibles y sus características.
     */
    public String obtenerPlanesActivosJSON() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT id_membresia, nombre, precio, descripcion FROM membresias WHERE activo = true ORDER BY precio ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                String desc = rs.getString("descripcion");
                if (desc != null) desc = desc.replace("\n", " ").replace("\r", "");
                else desc = "";

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_membresia")).append(",")
                        .append("\"nombre\":\"").append(rs.getString("nombre")).append("\",")
                        .append("\"precio\":").append(rs.getDouble("precio")).append(",")
                        .append("\"descripcion\":\"").append(desc).append("\"")
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    // ==========================================
    // REGLAS DE NEGOCIO PARA PLANES (NUEVO)
    // ==========================================

    /**
     * VALIDAR DUPLICIDAD DE NOMBRE DE PLAN (CREACIÓN)
     * Comprueba en la base de datos si ya existe un plan registrado con el mismo nombre
     * dentro de la misma sucursal para evitar ambigüedades en el catálogo.
     * Parametro nombre: Nombre del plan a evaluar.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Verdadero si ya existe un registro con ese nombre, falso si está disponible.
     */
    public boolean existeNombrePlan(String nombre, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM membresias WHERE LOWER(nombre) = LOWER(?) AND id_empresa = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * VALIDAR DUPLICIDAD DE NOMBRE DE PLAN (EDICIÓN)
     * Verifica que al cambiar el nombre de un plan existente, este no colisione
     * con el nombre de otro plan diferente ya registrado en la misma sucursal.
     * Parametro nombre: Nuevo nombre a validar.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro idMembresia: Identificador del plan que se está editando (para excluirlo de la búsqueda).
     * Retorna: Verdadero si el nombre ya está tomado por otro plan.
     */
    public boolean existeNombrePlanEdicion(String nombre, int idEmpresa, int idMembresia) {
        String sql = "SELECT COUNT(*) FROM membresias WHERE LOWER(nombre) = LOWER(?) AND id_empresa = ? AND id_membresia != ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, idEmpresa);
            ps.setInt(3, idMembresia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}