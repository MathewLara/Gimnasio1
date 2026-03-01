package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;

import java.sql.*;

/**
 * DAO para pagos (RF07)
 */
public class PagoDAO {

    /**
     * Lista todos los pagos (admin). Soporta filtro por idCliente si se pasa.
     */
    public String obtenerPagosJSON(Integer idCliente, int limite) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql;
            PreparedStatement ps;
            if (idCliente != null && idCliente > 0) {
                sql = "SELECT p.id_pago, p.fecha_pago, p.monto_pagado, p.metodo_pago, p.referencia_comprobante, " +
                        "m.nombre as membresia, c.nombre||' '||c.apellido as cliente " +
                        "FROM pagos p LEFT JOIN membresias m ON p.id_membresia = m.id_membresia " +
                        "LEFT JOIN clientes c ON p.id_cliente = c.id_cliente " +
                        "WHERE p.id_cliente = ? ORDER BY p.fecha_pago DESC LIMIT ?";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, idCliente);
                ps.setInt(2, limite);
            } else {
                sql = "SELECT p.id_pago, p.fecha_pago, p.monto_pagado, p.metodo_pago, p.referencia_comprobante, " +
                        "m.nombre as membresia, c.nombre||' '||c.apellido as cliente " +
                        "FROM pagos p LEFT JOIN membresias m ON p.id_membresia = m.id_membresia " +
                        "LEFT JOIN clientes c ON p.id_cliente = c.id_cliente " +
                        "ORDER BY p.fecha_pago DESC LIMIT ?";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, limite);
            }
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"idPago\":").append(rs.getInt("id_pago")).append(",")
                        .append("\"fecha\":\"").append(rs.getTimestamp("fecha_pago")).append("\",")
                        .append("\"monto\":").append(rs.getDouble("monto_pagado")).append(",")
                        .append("\"metodo\":\"").append(escape(rs.getString("metodo_pago"))).append("\",")
                        .append("\"referencia\":\"").append(escape(rs.getString("referencia_comprobante"))).append("\",")
                        .append("\"membresia\":\"").append(escape(rs.getString("membresia"))).append("\",")
                        .append("\"cliente\":\"").append(escape(rs.getString("cliente"))).append("\"")
                        .append("}");
                first = false;
            }
        } catch (SQLException e) {
            // Si no existe id_cliente en pagos, usar query sin ese join
            return obtenerPagosSinCliente(limite);
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    private String obtenerPagosSinCliente(int limite) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT p.id_pago, p.fecha_pago, p.monto_pagado, p.metodo_pago, m.nombre as membresia " +
                    "FROM pagos p LEFT JOIN membresias m ON p.id_membresia = m.id_membresia " +
                    "ORDER BY p.fecha_pago DESC LIMIT ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"idPago\":").append(rs.getInt("id_pago")).append(",")
                        .append("\"fecha\":\"").append(rs.getTimestamp("fecha_pago")).append("\",")
                        .append("\"monto\":").append(rs.getDouble("monto_pagado")).append(",")
                        .append("\"metodo\":\"").append(escape(rs.getString("metodo_pago"))).append("\",")
                        .append("\"membresia\":\"").append(escape(rs.getString("membresia"))).append("\"")
                        .append("}");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    private static String escape(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}
