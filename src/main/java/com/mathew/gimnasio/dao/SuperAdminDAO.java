package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SuperAdminDAO {

    // ==========================================
    // 1. DASHBOARD GENERAL
    // ==========================================
    public String getDashboardJSON() {
        StringBuilder json = new StringBuilder("{");
        try (Connection conn = ConexionDB.getConnection()) {
            int totalEmpresas = 0, totalUsuarios = 0;
            double totalIngresos = 0.0;

            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM empresas");
            if (rs.next()) totalEmpresas = rs.getInt(1);

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM usuarios WHERE id_rol IN (1,2,3,4)");
            if (rs.next()) totalUsuarios = rs.getInt(1);

            rs = conn.createStatement().executeQuery("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos");
            if (rs.next()) totalIngresos = rs.getDouble(1);

            json.append("\"totalEmpresas\":").append(totalEmpresas).append(",");
            json.append("\"totalUsuarios\":").append(totalUsuarios).append(",");
            json.append("\"totalIngresos\":").append(totalIngresos).append(",");

            json.append("\"ultimasEmpresas\":[");
            rs = conn.createStatement().executeQuery("SELECT nombre_empresa, activo FROM empresas ORDER BY id_empresa DESC LIMIT 5");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"nombre\":\"").append(rs.getString("nombre_empresa")).append("\",")
                        .append("\"estado\":\"").append(rs.getBoolean("activo") ? "Activo" : "Inactivo").append("\"")
                        .append("}");
                first = false;
            }
            json.append("]");

        } catch (Exception e) {
            return "{\"totalEmpresas\":0,\"totalUsuarios\":0,\"totalIngresos\":0,\"ultimasEmpresas\":[]}";
        }
        json.append("}");
        return json.toString();
    }

    // ==========================================
    // 2. GESTIÓN DE EMPRESAS
    // ==========================================
    public String getEmpresasJSON() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT e.id_empresa, e.nombre_empresa, e.ruc_nit, e.telefono, e.activo, e.fecha_registro, " +
                "(SELECT COUNT(*) FROM clientes c WHERE c.id_empresa = e.id_empresa) as total_clientes " +
                "FROM empresas e ORDER BY e.id_empresa ASC";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                String fecha = rs.getString("fecha_registro");
                if(fecha != null && fecha.length() > 10) fecha = fecha.substring(0, 10);

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_empresa")).append(",")
                        .append("\"nombre\":\"").append(rs.getString("nombre_empresa") != null ? rs.getString("nombre_empresa") : "").append("\",")
                        .append("\"ruc\":\"").append(rs.getString("ruc_nit") != null ? rs.getString("ruc_nit") : "").append("\",")
                        .append("\"telefono\":\"").append(rs.getString("telefono") != null ? rs.getString("telefono") : "").append("\",")
                        .append("\"estado\":").append(rs.getBoolean("activo")).append(",")
                        .append("\"fecha_registro\":\"").append(fecha != null ? fecha : "").append("\",")
                        .append("\"total_clientes\":").append(rs.getInt("total_clientes"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    public boolean guardarEmpresa(String nombre, String ruc, String telefono, String direccion) {
        String sql = "INSERT INTO empresas (nombre_empresa, ruc_nit, telefono, direccion, activo, fecha_registro) VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, ruc);
            ps.setString(3, telefono);
            ps.setString(4, direccion);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public boolean editarEmpresa(int id, String nombre, String ruc, String telefono, String direccion) {
        String sql = "UPDATE empresas SET nombre_empresa=?, ruc_nit=?, telefono=?, direccion=? WHERE id_empresa=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, ruc);
            ps.setString(3, telefono);
            ps.setString(4, direccion);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public boolean cambiarEstadoEmpresa(int id, boolean estado) {
        String sql = "UPDATE empresas SET activo=? WHERE id_empresa=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    // ==========================================
    // 3. ADMINISTRADORES LOCALES (DUEÑOS)
    // ==========================================
    public String getAdministradoresJSON() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT u.id_usuario, u.usuario, u.nombre, u.apellido, u.activo, e.nombre_empresa, " +
                "COALESCE(c.email, '') as email, COALESCE(c.telefono, '') as telefono " +
                "FROM usuarios u " +
                "JOIN empresas e ON u.id_empresa = e.id_empresa " +
                "LEFT JOIN clientes c ON u.id_usuario = c.id_usuario " +
                "WHERE u.id_rol = 1 ORDER BY u.id_usuario DESC";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_usuario")).append(",")
                        .append("\"usuario\":\"").append(rs.getString("usuario")).append("\",")
                        .append("\"nombre\":\"").append(rs.getString("nombre") != null ? rs.getString("nombre") : "").append("\",")
                        .append("\"apellido\":\"").append(rs.getString("apellido") != null ? rs.getString("apellido") : "").append("\",")
                        .append("\"empresa\":\"").append(rs.getString("nombre_empresa")).append("\",")
                        .append("\"email\":\"").append(rs.getString("email")).append("\",")
                        .append("\"telefono\":\"").append(rs.getString("telefono")).append("\",")
                        .append("\"estado\":").append(rs.getBoolean("activo"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    public boolean guardarAdmin(int idEmpresa, String nombre, String apellido, String email, String telefono, String usuario, String contrasena) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            // INICIAMOS TRANSACCIÓN: O se guarda todo perfecto, o no se guarda nada
            conn.setAutoCommit(false);

            String sql = "INSERT INTO usuarios (id_rol, id_empresa, usuario, contrasena, nombre, apellido, activo) VALUES (1, ?, ?, ?, ?, ?, TRUE)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idEmpresa);
                ps.setString(2, usuario);
                ps.setString(3, SecurityUtil.encriptar(contrasena));
                ps.setString(4, nombre);
                ps.setString(5, apellido);

                if (ps.executeUpdate() > 0) {
                    ResultSet rs = ps.getGeneratedKeys();
                    if(rs.next()){
                        int idUsuario = rs.getInt(1);

                        // CORRECCIÓN: Solo guardamos los campos que de verdad existen en la tabla clientes
                        String sqlCli = "INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono) VALUES (?, ?, ?, ?, ?)";
                        try(PreparedStatement psC = conn.prepareStatement(sqlCli)){
                            psC.setInt(1, idUsuario);
                            psC.setString(2, nombre);
                            psC.setString(3, apellido);
                            psC.setString(4, email);
                            psC.setString(5, telefono);
                            psC.executeUpdate();
                        }
                    }
                    conn.commit(); // TODO SALIÓ BIEN, GUARDAMOS DEFINITIVAMENTE
                    return true;
                }
            }
            conn.rollback(); // Si falló algo arriba, echamos para atrás el registro
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            if(conn != null) try { conn.rollback(); } catch(Exception ex){} // Rescate en caso de colapso
            return false;
        } finally {
            if(conn != null) try { conn.close(); } catch(Exception ex){}
        }
    }

    public boolean cambiarEstadoAdmin(int id, boolean estado) {
        String sql = "UPDATE usuarios SET activo=? WHERE id_usuario=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }
}