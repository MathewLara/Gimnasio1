package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;

public class RecepcionDAO {

    public String getDashboardRecepJSON() {
        StringBuilder json = new StringBuilder("{");

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. Ingresos en Caja (SOLO DE HOY)
            double cajaHoy = 0.0;
            String sqlCaja = "SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE DATE(fecha_pago) = CURRENT_DATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlCaja); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) cajaHoy = rs.getDouble(1);
            }

            // 2. Personas Entrenando (Aforo - Accesos exitosos de hoy)
            int aforoHoy = 0;
            String sqlAforo = "SELECT COUNT(DISTINCT id_usuario) FROM logs_acceso WHERE DATE(fecha_hora_log) = CURRENT_DATE AND exitoso = TRUE";
            try(PreparedStatement ps = conn.prepareStatement(sqlAforo); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) aforoHoy = rs.getInt(1);
            }

            // Armamos los KPIs
            json.append("\"kpis\": {")
                    .append("\"cajaHoy\": ").append(cajaHoy).append(",")
                    .append("\"aforoHoy\": ").append(aforoHoy)
                    .append("},");

            // 3. Actividad Reciente (Últimos 5 movimientos en puerta)
            json.append("\"actividadReciente\": [");
            String sqlActividad = "SELECT u.usuario, r.nombre_rol, l.fecha_hora_log, l.exitoso " +
                    "FROM logs_acceso l INNER JOIN usuarios u ON l.id_usuario = u.id_usuario " +
                    "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                    "ORDER BY l.fecha_hora_log DESC LIMIT 5";
            try(PreparedStatement ps = conn.prepareStatement(sqlActividad); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while(rs.next()) {
                    if(!first) json.append(",");
                    // Extraemos solo la hora de la fecha completa (ej: 14:30)
                    String horaCompleta = rs.getString("fecha_hora_log");
                    String soloHora = horaCompleta != null && horaCompleta.length() > 16 ? horaCompleta.substring(11, 16) : horaCompleta;

                    json.append("{")
                            .append("\"hora\": \"").append(soloHora).append("\",")
                            .append("\"cliente\": \"").append(rs.getString("usuario")).append("\",")
                            .append("\"rol\": \"").append(rs.getString("nombre_rol")).append("\",")
                            .append("\"exitoso\": ").append(rs.getBoolean("exitoso"))
                            .append("}");
                    first = false;
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
}