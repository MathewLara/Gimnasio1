package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.DashboardDTO;
import com.mathew.gimnasio.modelos.AccesoDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    // ==========================================
    // 1. OBTENER ESTADÍSTICAS
    // ==========================================
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
    // 4. GESTIÓN DE PLANES / MEMBRESÍAS (NUEVO)
    // ==========================================
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

    public boolean guardarPlan(String nombre, double precio, String descripcion, int idEmpresa) {
        String sql = "INSERT INTO membresias (nombre, precio, descripcion, id_empresa, activo) VALUES (?, ?, ?, ?, TRUE)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setDouble(2, precio);
            ps.setString(3, descripcion);
            ps.setInt(4, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean editarPlan(int id, String nombre, double precio, String descripcion, int idEmpresa) {
        String sql = "UPDATE membresias SET nombre=?, precio=?, descripcion=? WHERE id_membresia=? AND id_empresa=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setDouble(2, precio);
            ps.setString(3, descripcion);
            ps.setInt(4, id);
            ps.setInt(5, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean cambiarEstadoPlan(int id, boolean estado) {
        String sql = "UPDATE membresias SET activo=? WHERE id_membresia=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}