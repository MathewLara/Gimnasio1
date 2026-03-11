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

            // 1. Ingresos en Caja (SOLO DE HOY)
            double cajaHoy = 0.0;
            String sqlCaja = "SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE DATE(fecha_pago) = CURRENT_DATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlCaja); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) cajaHoy = rs.getDouble(1);
            }

            // 2. Personas Entrenando (Aforo actual)
            // CORREGIDO: Tabla 'asistencias' y columna 'fecha'
            int aforoHoy = 0;
            String sqlAforo = "SELECT COUNT(*) FROM asistencias WHERE fecha = CURRENT_DATE AND hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlAforo); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) aforoHoy = rs.getInt(1);
            }

            json.append("\"kpis\": {")
                    .append("\"cajaHoy\": ").append(cajaHoy).append(",")
                    .append("\"aforoHoy\": ").append(aforoHoy)
                    .append("},");

            // 3. Actividad Reciente FÍSICA
            // CORREGIDO: Tabla 'asistencias' y columna 'fecha'
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

            // 1. Buscar al usuario
            int idUsuario = -1;
            boolean activo = false;
            String nombreUsuario = "";

            String sqlUser = "SELECT id_usuario, usuario, activo FROM usuarios WHERE usuario = ?";
            try(PreparedStatement ps = conn.prepareStatement(sqlUser)) {
                ps.setString(1, identificador.trim());
                ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    idUsuario = rs.getInt("id_usuario");
                    nombreUsuario = rs.getString("usuario");
                    activo = rs.getBoolean("activo");
                }
            }

            if (idUsuario == -1) return "{\"status\":\"error\", \"mensaje\":\"Usuario no encontrado en la base de datos.\"}";
            if (!activo) return "{\"status\":\"error\", \"mensaje\":\"El usuario está inactivo. Verifique sus pagos.\"}";

            // 2. Ver si la persona ya está adentro del gimnasio
            // CORREGIDO: Tabla 'asistencias' y columna 'fecha'
            int idAsistencia = -1;
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_usuario = ? AND fecha = CURRENT_DATE AND hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, idUsuario);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) idAsistencia = rs.getInt("id_asistencia");
            }

            // 3. Tomar la decisión
            if (idAsistencia != -1) {
                // TIENE ENTRADA ABIERTA -> ¡ESTÁ SALIENDO DEL GIMNASIO!
                // CORREGIDO: Tabla 'asistencias'
                String sqlOut = "UPDATE asistencias SET hora_salida = CURRENT_TIME WHERE id_asistencia = ?";
                try(PreparedStatement ps = conn.prepareStatement(sqlOut)) {
                    ps.setInt(1, idAsistencia);
                    ps.executeUpdate();
                }
                return "{\"status\":\"ok\", \"tipo\":\"Salida\", \"mensaje\":\"¡Hasta pronto, " + nombreUsuario + "! Salida registrada.\"}";
            } else {
                // NO TIENE ENTRADA ABIERTA -> ¡ESTÁ ENTRANDO AL GIMNASIO!
                // CORREGIDO: Tabla 'asistencias' y columna 'fecha'
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