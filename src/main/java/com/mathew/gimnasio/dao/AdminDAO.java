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
     *
     * @return DashboardDTO con toda la telemetría poblada.
     */
    // 1. OBTENER ESTADÍSTICAS
    public DashboardDTO obtenerEstadisticas(int idEmpresa) { // <-- Recibe empresa
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
            } catch (Exception e) {
                e.printStackTrace();
            }
            dash.setUltimosAccesos(accesos);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception e) {
            }
        }
        return dash;
    }

    // 2. HISTORIAL DE PAGOS
    public String obtenerHistorialPagosJSON(int idEmpresa) { // <-- Recibe empresa
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT p.id_pago, u.usuario, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.id_membresia " +
                "FROM pagos p INNER JOIN clientes c ON p.id_cliente = c.id_cliente " +
                "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                "WHERE p.id_empresa = ? ORDER BY p.fecha_pago DESC"; // <-- El filtro mágico

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    // 3. REGISTRAR PAGO
    public boolean registrarPago(int idUsuario, int idPlan, double monto, String metodo, int idEmpresa) {
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago, id_empresa) " +
                "SELECT id_cliente, ?, ?, ?, CURRENT_TIMESTAMP, ? " +
                "FROM clientes WHERE id_usuario = ?";

        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            ps.setDouble(2, monto);
            ps.setString(3, metodo);
            ps.setInt(4, idEmpresa); // Inyectamos la empresa a la que va el dinero
            ps.setInt(5, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}