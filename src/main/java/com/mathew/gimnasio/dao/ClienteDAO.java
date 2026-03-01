package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.util.JsonUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para gestión de clientes (RF03)
 */
public class ClienteDAO {

    /**
     * Obtiene un cliente por id_cliente con datos de membresía.
     */
    public Map<String, Object> obtenerPorId(int idCliente) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT c.*, m.nombre as plan_nombre, m.precio as plan_precio, " +
                    "CASE WHEN c.fecha_vencimiento >= CURRENT_DATE THEN 'Activo' ELSE 'Vencido' END as estado " +
                    "FROM clientes c LEFT JOIN membresias m ON c.id_membresia = m.id_membresia WHERE c.id_cliente = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("idCliente", rs.getInt("id_cliente"));
                m.put("idUsuario", rs.getInt("id_usuario"));
                m.put("nombre", rs.getString("nombre"));
                m.put("apellido", rs.getString("apellido"));
                m.put("email", rs.getString("email"));
                m.put("telefono", rs.getString("telefono"));
                m.put("fechaNacimiento", rs.getDate("fecha_nacimiento"));
                m.put("idMembresia", rs.getObject("id_membresia"));
                m.put("fechaVencimiento", rs.getDate("fecha_vencimiento"));
                m.put("planNombre", rs.getString("plan_nombre"));
                m.put("planPrecio", rs.getDouble("plan_precio"));
                m.put("estadoMembresia", rs.getString("estado"));
                return m;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Actualiza datos de un cliente.
     */
    public boolean actualizar(int idCliente, String nombre, String apellido, String email, String telefono, java.sql.Date fechaNacimiento) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "UPDATE clientes SET nombre=?, apellido=?, email=?, telefono=?, fecha_nacimiento=? WHERE id_cliente=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setString(3, email);
            ps.setString(4, telefono);
            ps.setDate(5, fechaNacimiento);
            ps.setInt(6, idCliente);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Historial de pagos del cliente (RF03).
     * Ejecutar migracion_rf.sql para añadir id_cliente a pagos/factura_encabezados.
     */
    public String obtenerHistorialPagosJSON(int idCliente) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT p.id_pago, p.fecha_pago, p.monto_pagado, p.metodo_pago, p.referencia_comprobante, m.nombre as membresia " +
                    "FROM pagos p LEFT JOIN membresias m ON p.id_membresia = m.id_membresia " +
                    "WHERE p.id_cliente = ? ORDER BY p.fecha_pago DESC LIMIT 50";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"idPago\":").append(rs.getInt("id_pago")).append(",")
                        .append("\"fecha\":\"").append(rs.getTimestamp("fecha_pago")).append("\",")
                        .append("\"monto\":").append(rs.getDouble("monto_pagado")).append(",")
                        .append("\"metodo\":\"").append(JsonUtil.escape(rs.getString("metodo_pago"))).append("\",")
                        .append("\"referencia\":\"").append(JsonUtil.escape(rs.getString("referencia_comprobante"))).append("\",")
                        .append("\"membresia\":\"").append(JsonUtil.escape(rs.getString("membresia"))).append("\"")
                        .append("}");
                first = false;
            }
        } catch (SQLException e) {
            // Sin columna id_cliente: intentar vía factura_encabezados
            return obtenerHistorialPagosPorFactura(idCliente);
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    private String obtenerHistorialPagosPorFactura(int idCliente) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT p.id_pago, p.fecha_pago, p.monto_pagado, p.metodo_pago, f.numero_factura " +
                    "FROM pagos p JOIN factura_encabezados f ON p.id_pago = f.id_pago " +
                    "WHERE f.id_cliente = ? ORDER BY p.fecha_pago DESC LIMIT 50";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"idPago\":").append(rs.getInt("id_pago")).append(",")
                        .append("\"fecha\":\"").append(rs.getTimestamp("fecha_pago")).append("\",")
                        .append("\"monto\":").append(rs.getDouble("monto_pagado")).append(",")
                        .append("\"metodo\":\"").append(JsonUtil.escape(rs.getString("metodo_pago"))).append("\",")
                        .append("\"numeroFactura\":\"").append(JsonUtil.escape(rs.getString("numero_factura"))).append("\"")
                        .append("}");
                first = false;
            }
        } catch (SQLException e) {
            // Sin id_cliente en tablas
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

}
