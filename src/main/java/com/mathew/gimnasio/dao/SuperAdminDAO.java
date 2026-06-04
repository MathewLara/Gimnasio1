package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Empresa;
import com.mathew.gimnasio.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM usuarios WHERE id_rol = 1");
            if (rs.next()) totalUsuarios = rs.getInt(1);

            rs = conn.createStatement().executeQuery("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos");
            if (rs.next()) totalIngresos = rs.getDouble(1);

            json.append("\"totalEmpresas\":").append(totalEmpresas).append(",");
            json.append("\"totalUsuarios\":").append(totalUsuarios).append(",");
            json.append("\"totalIngresos\":").append(totalIngresos);
        } catch (Exception e) {
            return "{\"totalEmpresas\":0,\"totalUsuarios\":0,\"totalIngresos\":0}";
        }
        json.append("}");
        return json.toString();
    }

    // ==========================================
    // 2. GESTIÓN DE EMPRESAS
    // ==========================================
    public String getEmpresasJSON() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT e.id_empresa, e.nombre_empresa, e.ruc_nit, e.telefono, e.activo, " +
                "(SELECT COUNT(*) FROM clientes c WHERE c.id_empresa = e.id_empresa) as total_clientes " +
                "FROM empresas e ORDER BY e.id_empresa ASC";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_empresa")).append(",")
                        .append("\"nombre\":\"").append(rs.getString("nombre_empresa") != null ? rs.getString("nombre_empresa") : "").append("\",")
                        .append("\"ruc\":\"").append(rs.getString("ruc_nit") != null ? rs.getString("ruc_nit") : "").append("\",")
                        .append("\"telefono\":\"").append(rs.getString("telefono") != null ? rs.getString("telefono") : "").append("\",")
                        .append("\"estado\":").append(rs.getBoolean("activo")).append(",")
                        .append("\"total_clientes\":").append(rs.getInt("total_clientes"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    // Método robusto para insertar usando el objeto Empresa
    public boolean registrarEmpresa(Empresa empresa) {
        String sql = "INSERT INTO empresas (nombre_empresa, ruc_nit, telefono, direccion, activo, fecha_registro) VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empresa.getNombre().trim());
            ps.setString(2, empresa.getRuc().trim());
            ps.setString(3, empresa.getTelefono().trim());
            ps.setString(4, empresa.getDireccion().trim());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
    // VALIDACIONES DE NEGOCIO PARA EMPRESAS
    // ==========================================
    public boolean existeRuc(String ruc) {
        String sql = "SELECT COUNT(*) FROM empresas WHERE ruc_nit = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruc.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean existeNombreEmpresa(String nombre) {
        String sql = "SELECT COUNT(*) FROM empresas WHERE LOWER(nombre_empresa) = LOWER(?)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean existeTelefono(String telefono) {
        String sql = "SELECT COUNT(*) FROM empresas WHERE telefono = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, telefono.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean existeDireccion(String direccion) {
        String sql = "SELECT COUNT(*) FROM empresas WHERE LOWER(direccion) = LOWER(?)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, direccion.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean existeCorreo(String correo) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE LOWER(correo) = LOWER(?)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ==========================================
    // 3. ADMINISTRADORES LOCALES (DUEÑOS)
    // ==========================================
    public String getAdministradoresJSON() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT u.id_usuario, u.id_empresa, u.usuario, u.nombre, u.apellido, u.activo, e.nombre_empresa " +
                "FROM usuarios u JOIN empresas e ON u.id_empresa = e.id_empresa " +
                "WHERE u.id_rol = 1 ORDER BY u.id_usuario DESC";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_usuario")).append(",")
                        .append("\"id_empresa\":").append(rs.getInt("id_empresa")).append(",")
                        .append("\"usuario\":\"").append(rs.getString("usuario")).append("\",")
                        .append("\"nombre\":\"").append(rs.getString("nombre") != null ? rs.getString("nombre") : "").append("\",")
                        .append("\"apellido\":\"").append(rs.getString("apellido") != null ? rs.getString("apellido") : "").append("\",")
                        .append("\"empresa\":\"").append(rs.getString("nombre_empresa")).append("\",")
                        .append("\"estado\":").append(rs.getBoolean("activo"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    public boolean guardarAdmin(int idEmpresa, String nombre, String apellido, String usuario, String contrasena) {
        String sql = "INSERT INTO usuarios (id_rol, id_empresa, usuario, contrasena, nombre, apellido, activo) VALUES (1, ?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmpresa);
            ps.setString(2, usuario);
            ps.setString(3, SecurityUtil.encriptar(contrasena));
            ps.setString(4, nombre);
            ps.setString(5, apellido);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public boolean editarAdmin(int idUsuario, int idEmpresa, String nombre, String apellido, String usuario, String contrasena) {
        boolean cambiarPass = (contrasena != null && !contrasena.trim().isEmpty());
        String sql = cambiarPass ?
                "UPDATE usuarios SET id_empresa=?, nombre=?, apellido=?, usuario=?, contrasena=? WHERE id_usuario=?" :
                "UPDATE usuarios SET id_empresa=?, nombre=?, apellido=?, usuario=? WHERE id_usuario=?";

        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmpresa);
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setString(4, usuario);
            if (cambiarPass) {
                ps.setString(5, SecurityUtil.encriptar(contrasena));
                ps.setInt(6, idUsuario);
            } else {
                ps.setInt(5, idUsuario);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
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