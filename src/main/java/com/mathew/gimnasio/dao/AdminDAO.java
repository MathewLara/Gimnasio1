package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.DashboardDTO;
import com.mathew.gimnasio.modelos.AccesoDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public DashboardDTO obtenerEstadisticas() {
        DashboardDTO dash = new DashboardDTO();
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();

            // 1. Contar Total de Clientes
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalCuentas(rs.getInt(1));
            }

            // 2. Sumar Ingresos
            try(PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setIngresos(rs.getDouble(1));
            }

            // 3. Contar Entrenadores
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM entrenadores");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalEntrenadores(rs.getInt(1));
            }

            // 4. Llenar la Tabla de Accesos al Sistema (Nombres exactos)
            List<AccesoDTO> accesos = new ArrayList<>();

            // Usamos las columnas exactas de tu tabla: fecha_hora_log, direccion_ip, exitoso
            String sqlAccesos = "SELECT u.usuario, u.id_rol, a.fecha_hora_log, a.direccion_ip, a.exitoso " +
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

                    acc.setHora(rs.getString("fecha_hora_log"));
                    acc.setIp(rs.getString("direccion_ip"));

                    // Convertir el boolean de tu BD a texto para que se pinte en la web
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
            try { if(conn != null) conn.close(); } catch(Exception e) {}
        }
        return dash;
    }
    // ==========================================
    // OBTENER TODO EL HISTORIAL DE PAGOS (ADMIN)
    // ==========================================
    public String obtenerHistorialPagosJSON() {
        StringBuilder json = new StringBuilder("[");
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
                if(!first) json.append(",");
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

    // ==========================================
    // REGISTRAR PAGO (DESDE EL ADMIN)
    // ==========================================
    public boolean registrarPago(int idUsuario, int idPlan, double monto, String metodo) {
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago) " +
                "SELECT id_cliente, ?, ?, ?, CURRENT_TIMESTAMP " +
                "FROM clientes WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            ps.setDouble(2, monto);
            ps.setString(3, metodo);
            ps.setInt(4, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error Pago Admin: " + e.getMessage());
            return false;
        }
    }
}
