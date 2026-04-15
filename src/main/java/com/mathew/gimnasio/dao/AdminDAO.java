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
            // 4. NUEVO: Contar Membresías Vencidas
            String sqlVencidas = "SELECT COUNT(*) FROM clientes WHERE fecha_vencimiento < CURRENT_DATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlVencidas);
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setMembresiasVencidas(rs.getInt(1));
            }
            // 5. Últimos Accesos (CORREGIDO CON HORA DE ECUADOR Y FORMATO LIMPIO)
            List<AccesoDTO> accesos = new ArrayList<>();
            // El truco está aquí: Restamos 5 horas (INTERVAL '5 hours') y le damos formato limpio (TO_CHAR)
            String sqlAccesos = "SELECT u.usuario, " +
                    "TO_CHAR(a.fecha_hora_ingreso - INTERVAL '5 hours', 'HH24:MI:SS') AS ingreso_ec, " +
                    "TO_CHAR(a.fecha_hora_salida - INTERVAL '5 hours', 'HH24:MI:SS') AS salida_ec " +
                    "FROM asistencias a " +
                    "INNER JOIN clientes c ON a.id_cliente = c.id_cliente " +
                    "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                    "ORDER BY a.fecha_hora_ingreso DESC LIMIT 10";

            try(PreparedStatement ps = conn.prepareStatement(sqlAccesos);
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    AccesoDTO acc = new AccesoDTO();
                    acc.setUsuario(rs.getString("usuario"));
                    acc.setRol("Cliente");
                    acc.setHoraIngreso(rs.getString("ingreso_ec"));

                    // Si aún no ha salido, mostrar "En curso..."
                    String salida = rs.getString("salida_ec");
                    acc.setHoraSalida(salida != null ? salida : "---");

                    acc.setEstado(salida != null ? "Completado" : "Entrenando");
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
