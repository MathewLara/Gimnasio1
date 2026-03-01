package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DAO para logs de acceso al sistema (RF09)
 */
public class LogAccesoDAO {

    public void registrarAcceso(int idUsuario, boolean exitoso) {
        registrarAcceso(idUsuario, null, null, exitoso);
    }

    public void registrarAcceso(int idUsuario, String direccionIp, String tipoDispositivo, boolean exitoso) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "INSERT INTO logs_acceso (id_usuario, direccion_ip, tipo_dispositivo, exitoso) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ps.setString(2, direccionIp);
            ps.setString(3, tipoDispositivo);
            ps.setBoolean(4, exitoso);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtiene los últimos logs de acceso en JSON.
     */
    public String obtenerLogsJSON(int limite) {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT l.id_log, l.id_usuario, u.usuario, r.nombre_rol, " +
                    "l.fecha_hora_log, l.direccion_ip, l.tipo_dispositivo, l.exitoso " +
                    "FROM logs_acceso l " +
                    "LEFT JOIN usuarios u ON l.id_usuario = u.id_usuario " +
                    "LEFT JOIN roles r ON u.id_rol = r.id_rol " +
                    "ORDER BY l.fecha_hora_log DESC LIMIT ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"idLog\":").append(rs.getInt("id_log")).append(",")
                        .append("\"idUsuario\":").append(rs.getInt("id_usuario")).append(",")
                        .append("\"usuario\":\"").append(escape(rs.getString("usuario"))).append("\",")
                        .append("\"rol\":\"").append(escape(rs.getString("nombre_rol"))).append("\",")
                        .append("\"fechaHora\":\"").append(rs.getTimestamp("fecha_hora_log")).append("\",")
                        .append("\"ip\":\"").append(escape(rs.getString("direccion_ip"))).append("\",")
                        .append("\"dispositivo\":\"").append(escape(rs.getString("tipo_dispositivo"))).append("\",")
                        .append("\"exitoso\":").append(rs.getBoolean("exitoso"))
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
