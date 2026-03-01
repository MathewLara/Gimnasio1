package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.util.JsonUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DAO para reportes administrativos (RF08)
 */
public class ReporteDAO {

    /**
     * Reporte de asistencia: total por día en un rango de fechas.
     */
    public String reporteAsistencia(String fechaInicio, String fechaFin) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT DATE(fecha_hora_ingreso) as dia, COUNT(*) as total " +
                    "FROM asistencias WHERE DATE(fecha_hora_ingreso) BETWEEN ? AND ? " +
                    "GROUP BY DATE(fecha_hora_ingreso) ORDER BY dia";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDate(1, toDate(fechaInicio));
            ps.setDate(2, toDate(fechaFin));
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"fecha\":\"").append(rs.getDate("dia")).append("\",\"total\":").append(rs.getInt("total")).append("}");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Reporte de ingresos: total por día en un rango.
     */
    public String reporteIngresos(String fechaInicio, String fechaFin) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT DATE(fecha_pago) as dia, SUM(monto_pagado) as total " +
                    "FROM pagos WHERE DATE(fecha_pago) BETWEEN ? AND ? " +
                    "GROUP BY DATE(fecha_pago) ORDER BY dia";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDate(1, toDate(fechaInicio));
            ps.setDate(2, toDate(fechaFin));
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"fecha\":\"").append(rs.getDate("dia")).append("\",\"total\":").append(rs.getDouble("total")).append("}");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Reporte de rutinas activas: rutinas y clientes.
     */
    public String reporteRutinas() {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT r.id_rutina, r.nombre_rutina, c.nombre||' '||c.apellido as cliente, " +
                    "e.nombre||' '||e.apellido as entrenador, r.fecha_creacion " +
                    "FROM rutinas r JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "WHERE r.activa = TRUE ORDER BY r.fecha_creacion DESC";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"idRutina\":").append(rs.getInt("id_rutina"))
                        .append(",\"nombreRutina\":\"").append(JsonUtil.escape(rs.getString("nombre_rutina")))
                        .append("\",\"cliente\":\"").append(JsonUtil.escape(rs.getString("cliente")))
                        .append("\",\"entrenador\":\"").append(JsonUtil.escape(rs.getString("entrenador")))
                        .append("\",\"fechaCreacion\":\"").append(rs.getDate("fecha_creacion")).append("\"}");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    private static java.sql.Date toDate(String s) {
        if (s == null || s.isEmpty()) return java.sql.Date.valueOf(LocalDate.now().minusMonths(1));
        try {
            return java.sql.Date.valueOf(s);
        } catch (Exception e) {
            return java.sql.Date.valueOf(LocalDate.now().minusMonths(1));
        }
    }

}
