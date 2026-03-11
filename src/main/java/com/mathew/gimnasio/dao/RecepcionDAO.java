package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;

public class RecepcionDAO {

    // ==========================================
    // 1. CARGAR DASHBOARD (AFORO Y CAJA)
    // ==========================================
    public String getDashboardRecepJSON() {
        StringBuilder json = new StringBuilder("{");

        try (Connection conn = ConexionDB.getConnection()) {

            double cajaHoy = 0.0;
            String sqlCaja = "SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE DATE(fecha_pago) = CURRENT_DATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlCaja); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) cajaHoy = rs.getDouble(1);
            }

            int aforoHoy = 0;
            String sqlAforo = "SELECT COUNT(*) FROM asistencias WHERE fecha = CURRENT_DATE AND hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlAforo); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) aforoHoy = rs.getInt(1);
            }

            json.append("\"kpis\": {")
                    .append("\"cajaHoy\": ").append(cajaHoy).append(",")
                    .append("\"aforoHoy\": ").append(aforoHoy)
                    .append("},");

            json.append("\"actividadReciente\": [");
            String sqlActividad = "SELECT u.usuario, a.hora_entrada, a.hora_salida " +
                    "FROM asistencias a INNER JOIN usuarios u ON a.id_usuario = u.id_usuario " +
                    "WHERE a.fecha = CURRENT_DATE " +
                    "ORDER BY COALESCE(a.hora_salida, a.hora_entrada) DESC LIMIT 5";
            try(PreparedStatement ps = conn.prepareStatement(sqlActividad); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while(rs.next()) {
                    if(!first) json.append(",");

                    String horaIn = rs.getString("hora_entrada");
                    String horaOut = rs.getString("hora_salida");

                    boolean esSalida = (horaOut != null);
                    String horaMovimiento = esSalida ? horaOut : horaIn;

                    if(horaMovimiento != null && horaMovimiento.length() >= 5) {
                        horaMovimiento = horaMovimiento.substring(0, 5);
                    }

                    json.append("{")
                            .append("\"hora\": \"").append(horaMovimiento).append("\",")
                            .append("\"cliente\": \"").append(rs.getString("usuario")).append("\",")
                            .append("\"tipo\": \"").append(esSalida ? "Salida" : "Entrada").append("\"")
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

    // ==========================================
    // 2. PROCESAR EL ESCÁNER QR (ENTRADA / SALIDA)
    // ==========================================
    public String procesarAccesoQr(String identificador) {
        try (Connection conn = ConexionDB.getConnection()) {

            int idUsuario = -1;
            boolean activo = false;
            String nombreUsuario = "";

            String paramLimpio = identificador.trim().toLowerCase();
            int idBuscado = -1;

            // ¡MAGIA!: Si el QR dice "iron_19", extraemos solo el número "19"
            if (paramLimpio.startsWith("iron_")) {
                try {
                    idBuscado = Integer.parseInt(paramLimpio.substring(5));
                } catch (Exception e) {}
            } else {
                // Por si tipean el número directamente (Ej: "19")
                try {
                    idBuscado = Integer.parseInt(paramLimpio);
                } catch (Exception e) {}
            }

            // Ahora la consulta busca por Usuario, Email o directamente por ID
            String sqlUser = "SELECT u.id_usuario, u.usuario, u.activo " +
                    "FROM usuarios u " +
                    "LEFT JOIN clientes c ON u.id_usuario = c.id_usuario " +
                    "LEFT JOIN entrenadores e ON u.id_usuario = e.id_usuario " +
                    "WHERE LOWER(u.usuario) = ? OR LOWER(c.email) = ? OR LOWER(e.email) = ? OR u.id_usuario = ?";

            try(PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setString(1, paramLimpio);
                ps.setString(2, paramLimpio);
                ps.setString(3, paramLimpio);
                ps.setInt(4, idBuscado);

                ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    idUsuario = rs.getInt("id_usuario");
                    nombreUsuario = rs.getString("usuario");
                    activo = rs.getBoolean("activo");
                }
            }

            if (idUsuario == -1) return "{\"status\":\"error\", \"mensaje\":\"Usuario no encontrado en la base de datos.\"}";
            if (!activo) return "{\"status\":\"error\", \"mensaje\":\"El usuario está inactivo. Verifique sus pagos.\"}";

            int idAsistencia = -1;
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_usuario = ? AND fecha = CURRENT_DATE AND hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, idUsuario);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) idAsistencia = rs.getInt("id_asistencia");
            }

            if (idAsistencia != -1) {
                // TIENE ENTRADA ABIERTA -> SALIDA
                String sqlOut = "UPDATE asistencias SET hora_salida = CURRENT_TIME WHERE id_asistencia = ?";
                try(PreparedStatement ps = conn.prepareStatement(sqlOut)) {
                    ps.setInt(1, idAsistencia);
                    ps.executeUpdate();
                }
                return "{\"status\":\"ok\", \"tipo\":\"Salida\", \"mensaje\":\"¡Hasta pronto, " + nombreUsuario + "! Salida registrada.\"}";
            } else {
                // NO TIENE ENTRADA ABIERTA -> ENTRADA
                String sqlIn = "INSERT INTO asistencias (id_usuario, fecha, hora_entrada) VALUES (?, CURRENT_DATE, CURRENT_TIME)";
                try(PreparedStatement ps = conn.prepareStatement(sqlIn)) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }
                return "{\"status\":\"ok\", \"tipo\":\"Entrada\", \"mensaje\":\"¡Bienvenido a entrenar, " + nombreUsuario + "! Entrada registrada.\"}";
            }

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if(errorMsg != null) {
                errorMsg = errorMsg.replace("\"", "'").replace("\n", " ");
            } else {
                errorMsg = "Error desconocido";
            }
            System.out.println("Error procesando QR: " + errorMsg);
            return "{\"status\":\"error\", \"mensaje\":\"Error BD: " + errorMsg + "\"}";
        }
    }
}