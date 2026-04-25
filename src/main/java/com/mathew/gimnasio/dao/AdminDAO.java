package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.DashboardDTO;
import com.mathew.gimnasio.modelos.AccesoDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DATA ACCESS OBJECT (DAO) DE ADMINISTRACIÓN
 * Esta clase concentra toda la lógica de persistencia exclusiva del rol Administrador.
 * Utiliza JDBC puro (Java Database Connectivity) para garantizar el máximo rendimiento
 * en consultas estadísticas pesadas y cruces de tablas (JOINs).
 */
public class AdminDAO {

    /**
     * OBTENER ESTADÍSTICAS GERENCIALES (KPIs)
     * Compila los indicadores clave de rendimiento del gimnasio ejecutando múltiples
     * consultas ligeras y empaquetándolas en un único objeto de transferencia (DashboardDTO).
     * @return DashboardDTO con toda la telemetría poblada.
     */
    public DashboardDTO obtenerEstadisticas() {
        DashboardDTO dash = new DashboardDTO();
        Connection conn = null;
        try {
            // Obtenemos la conexión del Singleton (Patrón Factory)
            conn = ConexionDB.getConnection();

            // 1. Contar Total de Clientes
            // Uso de Try-with-resources para cerrar automáticamente el PreparedStatement y ResultSet
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalCuentas(rs.getInt(1));
            }

            // 2. Sumar Ingresos
            // COALESCE evita que la base de datos devuelva NULL si no hay pagos registrados, devolviendo 0
            try(PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setIngresos(rs.getDouble(1));
            }

            // 3. Contar Entrenadores activos
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM entrenadores");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalEntrenadores(rs.getInt(1));
            }

            // 4. NUEVO: Contar Membresías Vencidas
            // Evalúa qué clientes tienen una fecha de corte anterior al día actual del servidor
            String sqlVencidas = "SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento < CURRENT_DATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlVencidas);
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setMembresiasVencidas(rs.getInt(1));
            }

            // 5. Últimos Accesos (BLINDADO Y CON HORA DE ECUADOR)
            List<AccesoDTO> accesos = new ArrayList<>();

            // Ajuste Arquitectónico de TimeZone:
            // Restamos 5 horas directamente en la base de datos (PostgreSQL) usando INTERVAL
            // para formatear la fecha_hora_log a la zona horaria GMT-5 (Ecuador).
            String sqlAccesos = "SELECT u.usuario, u.id_rol, " +
                    "TO_CHAR(a.fecha_hora_log - INTERVAL '5 hours', 'YYYY-MM-DD HH24:MI:SS') AS fecha_ec, " +
                    "a.direccion_ip, a.exitoso " +
                    "FROM logs_acceso a " +
                    "INNER JOIN usuarios u ON a.id_usuario = u.id_usuario " +
                    "ORDER BY a.fecha_hora_log DESC LIMIT 5";

            try(PreparedStatement ps = conn.prepareStatement(sqlAccesos);
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    AccesoDTO acc = new AccesoDTO();
                    acc.setUsuario(rs.getString("usuario"));

                    int idRol = rs.getInt("id_rol");
                    acc.setRol(idRol == 1 ? "Admin" : "Cliente");

                    // 1. Tomamos la hora ya ajustada a Ecuador. Si está vacía, ponemos "---" para que no se rompa la vista.
                    String horaAjustada = rs.getString("fecha_ec");
                    acc.setHoraIngreso(horaAjustada != null ? horaAjustada : "---");

                    // 2. Tomamos la IP. Si llega nula de la base de datos, ponemos un valor por defecto.
                    String ip = rs.getString("direccion_ip");
                    acc.setIp(ip != null ? ip : "Desconocida");

                    boolean esExitoso = rs.getBoolean("exitoso");
                    acc.setEstado(esExitoso ? "Exitoso" : "Fallido");

                    accesos.add(acc);
                }
            } catch (Exception e) {
                System.out.println("Error en la consulta de accesos: " + e.getMessage());
            }
            dash.setUltimosAccesos(accesos);

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            // Cierre seguro de la conexión maestra
            try { if(conn != null) conn.close(); } catch(Exception e) {}
        }
        return dash;
    }

    /**
     * OBTENER TODO EL HISTORIAL DE PAGOS (ADMIN)
     * Construye un JSON manualmente utilizando StringBuilder.
     * Esta técnica evita instanciar decenas de DTOs, maximizando la velocidad
     * de respuesta en historiales de facturación muy grandes.
     * @return String con formato JSON Array.
     */
    // ==========================================
    // OBTENER TODO EL HISTORIAL DE PAGOS (ADMIN)
    // ==========================================
    public String obtenerHistorialPagosJSON() {
        StringBuilder json = new StringBuilder("[");
        // Cruce relacional: Pagos -> Clientes -> Usuarios (Para obtener el username real)
        String sql = "SELECT p.id_pago, u.usuario, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.id_membresia " +
                "FROM pagos p " +
                "INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "ORDER BY p.fecha_pago DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while(rs.next()) {
                if(!first) json.append(","); // Evita la coma extra al final del array JSON
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
        } catch (Exception e) {
            System.out.println("Error Historial Pagos Admin: " + e.getMessage());
        }
        json.append("]");
        return json.toString();
    }

    /**
     * REGISTRAR PAGO (DESDE EL ADMIN)
     * Utiliza un INSERT condicionado (INSERT ... SELECT) para resolver la
     * clave foránea (id_cliente) en una sola consulta directamente en el motor SQL.
     */
    // ==========================================
    // REGISTRAR PAGO (DESDE EL ADMIN)
    // ==========================================
    public boolean registrarPago(int idUsuario, int idPlan, double monto, String metodo) {
        // La subconsulta SELECT busca el id_cliente asociado al id_usuario enviado desde la web
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago) " +
                "SELECT id_cliente, ?, ?, ?, CURRENT_TIMESTAMP " +
                "FROM clientes WHERE id_usuario = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPlan);
            ps.setDouble(2, monto);
            ps.setString(3, metodo);
            ps.setInt(4, idUsuario);

            // Retorna true si se insertó al menos 1 fila
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error Pago Admin: " + e.getMessage());
            return false;
        }
    }
}