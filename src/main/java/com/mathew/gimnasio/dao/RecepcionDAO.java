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

            // 1. Ingresos en Caja
            double cajaHoy = 0.0;
            String sqlCaja = "SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE DATE(fecha_pago) = CURRENT_DATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlCaja); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) cajaHoy = rs.getDouble(1);
            }

            // 2. Personas Entrenando (Aforo actual)
            int aforoHoy = 0;
            String sqlAforo = "SELECT COUNT(*) FROM asistencias WHERE DATE(fecha_hora_ingreso) = CURRENT_DATE AND fecha_hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlAforo); ResultSet rs = ps.executeQuery()) {
                if(rs.next()) aforoHoy = rs.getInt(1);
            }

            json.append("\"kpis\": {")
                    .append("\"cajaHoy\": ").append(cajaHoy).append(",")
                    .append("\"aforoHoy\": ").append(aforoHoy)
                    .append("},");

            // 3. Actividad Reciente FÍSICA (SEPARANDO ENTRADAS Y SALIDAS CON UNION)
            json.append("\"actividadReciente\": [");
            String sqlActividad =
                    "SELECT u.usuario, a.fecha_hora_ingreso AS hora_movimiento, 'Entrada' AS tipo_movimiento " +
                            "FROM asistencias a " +
                            "INNER JOIN clientes c ON a.id_cliente = c.id_cliente " +
                            "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                            "WHERE DATE(a.fecha_hora_ingreso) = CURRENT_DATE " +
                            "UNION ALL " +
                            "SELECT u.usuario, a.fecha_hora_salida AS hora_movimiento, 'Salida' AS tipo_movimiento " +
                            "FROM asistencias a " +
                            "INNER JOIN clientes c ON a.id_cliente = c.id_cliente " +
                            "INNER JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                            "WHERE a.fecha_hora_salida IS NOT NULL AND DATE(a.fecha_hora_salida) = CURRENT_DATE " +
                            "ORDER BY hora_movimiento DESC LIMIT 5";

            try(PreparedStatement ps = conn.prepareStatement(sqlActividad); ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while(rs.next()) {
                    if(!first) json.append(",");

                    String horaMovimiento = rs.getString("hora_movimiento");
                    String tipoMovimiento = rs.getString("tipo_movimiento");

                    // Extraemos solo la hora (ej. 14:30)
                    if(horaMovimiento != null && horaMovimiento.length() >= 16) {
                        horaMovimiento = horaMovimiento.substring(11, 16);
                    }

                    json.append("{")
                            .append("\"hora\": \"").append(horaMovimiento).append("\",")
                            .append("\"cliente\": \"").append(rs.getString("usuario")).append("\",")
                            .append("\"tipo\": \"").append(tipoMovimiento).append("\"")
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
            int idCliente = -1;
            boolean activo = false;
            String nombreUsuario = "";

            String paramLimpio = identificador.trim().toLowerCase();
            int idBuscado = -1;

            if (paramLimpio.startsWith("iron_")) {
                try { idBuscado = Integer.parseInt(paramLimpio.substring(5)); } catch (Exception e) {}
            } else {
                try { idBuscado = Integer.parseInt(paramLimpio); } catch (Exception e) {}
            }

            String sqlUser = "SELECT u.id_usuario, c.id_cliente, u.usuario, u.activo " +
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
                    idCliente = rs.getInt("id_cliente");
                    nombreUsuario = rs.getString("usuario");
                    activo = rs.getBoolean("activo");
                }
            }

            if (idUsuario == -1) return "{\"status\":\"error\", \"mensaje\":\"Usuario no encontrado en la base de datos.\"}";
            if (!activo) return "{\"status\":\"error\", \"mensaje\":\"El usuario está inactivo. Verifique sus pagos.\"}";
            if (idCliente <= 0) return "{\"status\":\"error\", \"mensaje\":\"El usuario existe pero no está registrado como cliente.\"}";

            int idAsistencia = -1;
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_cliente = ? AND DATE(fecha_hora_ingreso) = CURRENT_DATE AND fecha_hora_salida IS NULL";
            try(PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, idCliente);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) idAsistencia = rs.getInt("id_asistencia");
            }

            if (idAsistencia != -1) {
                // SALIDA
                String sqlOut = "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP WHERE id_asistencia = ?";
                try(PreparedStatement ps = conn.prepareStatement(sqlOut)) {
                    ps.setInt(1, idAsistencia);
                    ps.executeUpdate();
                }
                return "{\"status\":\"ok\", \"tipo\":\"Salida\", \"mensaje\":\"¡Hasta pronto, " + nombreUsuario + "! Salida registrada.\"}";
            } else {
                // ENTRADA
                // TRUCO: Le agregamos los milisegundos al código para que PostgreSQL no bloquee por ser duplicado
                String codigoUnico = identificador.trim() + "_" + System.currentTimeMillis();

                String sqlIn = "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso, dispositivo_qr, codigo_validado) VALUES (?, CURRENT_TIMESTAMP, 'Escáner Recepción', ?)";
                try(PreparedStatement ps = conn.prepareStatement(sqlIn)) {
                    ps.setInt(1, idCliente);
                    ps.setString(2, codigoUnico); // Ahora siempre será único
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
    // ==========================================
    // 3. OBTENER DIRECTORIO DE SOCIOS (CON ESTADO EN VIVO)
    // ==========================================
    public String obtenerSociosRecepcionJSON() {
        StringBuilder json = new StringBuilder("[");
        // Consulta avanzada: Busca a los clientes y cuenta si están adentro del gimnasio HOY
        String sql = "SELECT u.id_usuario, u.usuario, u.nombre, u.apellido, u.activo, " +
                "c.email, c.telefono, " +
                "(SELECT COUNT(*) FROM asistencias a WHERE a.id_cliente = c.id_cliente AND DATE(a.fecha_hora_ingreso) = CURRENT_DATE AND a.fecha_hora_salida IS NULL) as esta_entrenando " +
                "FROM usuarios u " +
                "INNER JOIN clientes c ON u.id_usuario = c.id_usuario " +
                "WHERE u.id_rol = 4 " +
                "ORDER BY u.id_usuario DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_usuario")).append(",")
                        .append("\"usuario\":\"").append(rs.getString("usuario")).append("\",")
                        .append("\"nombre\":\"").append(rs.getString("nombre") != null ? rs.getString("nombre") : "").append("\",")
                        .append("\"apellido\":\"").append(rs.getString("apellido") != null ? rs.getString("apellido") : "").append("\",")
                        .append("\"activo\":").append(rs.getBoolean("activo")).append(",")
                        .append("\"email\":\"").append(rs.getString("email") != null ? rs.getString("email") : "").append("\",")
                        .append("\"telefono\":\"").append(rs.getString("telefono") != null ? rs.getString("telefono") : "").append("\",")
                        .append("\"esta_entrenando\":").append(rs.getInt("esta_entrenando"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }
}