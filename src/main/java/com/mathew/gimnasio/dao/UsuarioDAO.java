package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Usuario;
import com.mathew.gimnasio.util.SecurityUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // ================================================================
    // 1. REGISTRO ATÓMICO (Usuario + Cliente + Código en UNA SOLA VEZ)
    // ================================================================
    public boolean registrarNuevoUsuario(Usuario u, String codigoGenerado) {
        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psCliente = null;
        PreparedStatement psCodigo = null;
        ResultSet rs = null;

        System.out.println(">>> DAO: Iniciando Transacción de Registro Completo <<<");

        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // ¡IMPORTANTE! Inicio de transacción

            // --- A. INSERTAR USUARIO ---
            String sqlUsuario = "INSERT INTO usuarios (id_rol, usuario, contrasena, activo) VALUES (?, ?, ?, ?)";
            psUsuario = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS);

            psUsuario.setInt(1, u.getIdRol());
            psUsuario.setString(2, u.getUsuario());
            psUsuario.setString(3, SecurityUtil.encriptar(u.getContrasena()));
            psUsuario.setBoolean(4, false); // Nace inactivo

            if (psUsuario.executeUpdate() == 0) throw new SQLException("Fallo al insertar usuario.");

            rs = psUsuario.getGeneratedKeys();
            int idGenerado = 0;
            if (rs.next()) {
                idGenerado = rs.getInt(1);
                u.setIdUsuario(idGenerado);
            }

            // --- B. INSERTAR CLIENTE ---
            String nombre = (u.getNombre() != null) ? u.getNombre() : "N/A";
            String apellido = (u.getApellido() != null) ? u.getApellido() : "N/A";
            String emailLimpio = u.getEmail().trim().toLowerCase();

            java.sql.Date fechaSql = null;
            if (u.getFechaNacimiento() != null && !u.getFechaNacimiento().isEmpty()) {
                try { fechaSql = java.sql.Date.valueOf(u.getFechaNacimiento()); } catch (Exception e) {}
            }

            String sqlCliente = "INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono, fecha_nacimiento) VALUES (?, ?, ?, ?, ?, ?)";
            psCliente = conn.prepareStatement(sqlCliente);
            psCliente.setInt(1, idGenerado);
            psCliente.setString(2, nombre);
            psCliente.setString(3, apellido);
            psCliente.setString(4, emailLimpio);
            psCliente.setString(5, u.getTelefono());
            psCliente.setDate(6, fechaSql);

            psCliente.executeUpdate();

            // --- C. INSERTAR CÓDIGO (¡AQUÍ MISMO!) ---
            System.out.println(">>> DAO: Guardando código " + codigoGenerado + " para ID " + idGenerado);
            String sqlCodigo = "INSERT INTO codigos_verificacion (id_usuario, codigo, fecha_expiracion) VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '15 minutes')";
            psCodigo = conn.prepareStatement(sqlCodigo);
            psCodigo.setInt(1, idGenerado);
            psCodigo.setString(2, codigoGenerado);
            psCodigo.executeUpdate();

            // --- CONFIRMAR TODO ---
            conn.commit();
            System.out.println(">>> DAO: ¡Registro Completado Exitosamente! <<<");
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psUsuario != null) psUsuario.close(); } catch (Exception e) {}
            try { if (psCliente != null) psCliente.close(); } catch (Exception e) {}
            try { if (psCodigo != null) psCodigo.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    // ==========================================
    // 2. VALIDAR CÓDIGO POR EMAIL
    // ==========================================
    public boolean validarCodigoPorEmail(String email, String codigoIngresado) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        System.out.println("--- DAO: Validando Código ---");
        System.out.println("Email buscado: " + email);
        System.out.println("Código ingresado: " + codigoIngresado);

        try {
            conn = ConexionDB.getConnection();
            String emailLimpio = email.trim().toLowerCase();

            // A. Buscar ID
            String sqlBuscarId = "SELECT id_usuario FROM clientes WHERE LOWER(email) = ? UNION SELECT id_usuario FROM entrenadores WHERE LOWER(email) = ?";
            ps = conn.prepareStatement(sqlBuscarId);
            ps.setString(1, emailLimpio);
            ps.setString(2, emailLimpio);
            rs = ps.executeQuery();

            int idEncontrado = -1;
            if (rs.next()) idEncontrado = rs.getInt("id_usuario");
            rs.close(); ps.close();

            if (idEncontrado == -1) {
                System.out.println("ERROR: Email no registrado.");
                return false;
            }
            System.out.println("INFO: ID encontrado: " + idEncontrado);

            // B. Validar Código
            String sqlValidar = "SELECT id_codigo, codigo FROM codigos_verificacion WHERE id_usuario = ? AND codigo = ? AND usado = FALSE";
            ps = conn.prepareStatement(sqlValidar);
            ps.setInt(1, idEncontrado);
            ps.setString(2, codigoIngresado.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("¡ÉXITO! Código coincide.");
                int idCodigo = rs.getInt("id_codigo");

                // Quemar código y Activar usuario
                ps.close();
                ps = conn.prepareStatement("UPDATE codigos_verificacion SET usado = TRUE WHERE id_codigo = ?");
                ps.setInt(1, idCodigo);
                ps.executeUpdate();

                activarUsuario(idEncontrado);
                return true;
            } else {
                System.out.println("ERROR: Código incorrecto en BDD.");
                // Debugging: Ver qué tiene la BDD realmente
                verCodigosDeUsuario(idEncontrado);
            }

        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(conn!=null) conn.close(); } catch(Exception e){} }
        return false;
    }

    // ==========================================
    // 3. ACTIVAR USUARIO
    // ==========================================
    public boolean activarUsuario(int idUsuario) {
        String sql = "UPDATE usuarios SET activo = TRUE WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // ==========================================
    // 4. LOGIN
    // ==========================================
    public Usuario login(String userOrEmail, String pass) {
        String sql = "SELECT u.*, c.email as email_cliente, e.email as email_entrenador " +
                "FROM usuarios u " +
                "LEFT JOIN clientes c ON u.id_usuario = c.id_usuario " +
                "LEFT JOIN entrenadores e ON u.id_usuario = e.id_usuario " +
                "WHERE LOWER(u.usuario) = LOWER(?) OR LOWER(c.email) = LOWER(?) OR LOWER(e.email) = LOWER(?)";

        Usuario u = null;
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String term = userOrEmail.trim();
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Verificar contraseña
                if (SecurityUtil.verificar(pass, rs.getString("contrasena"))) {
                    u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setUsuario(rs.getString("usuario"));
                    u.setActivo(rs.getBoolean("activo"));

                    // Recuperar el email correcto
                    String email = rs.getString("email_cliente");
                    if (email == null) email = rs.getString("email_entrenador");
                    u.setEmail(email);

                    u.setContrasena(null); // Seguridad
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return u;
    }

    // ==========================================
    // 5. MÉTODOS AUXILIARES Y STUBS
    // ==========================================

    // Método para guardar código fuera del registro (ej. reenviar código)
    public boolean guardarCodigo2FA(int idUsuario, String codigo) {
        String sql = "INSERT INTO codigos_verificacion (id_usuario, codigo, fecha_expiracion) VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '15 minutes')";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, codigo);
            ps.executeUpdate();
            return true;
        } catch (Exception e) { return false; }
    }

    // Debugging
    private void verCodigosDeUsuario(int idUsuario) {
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM codigos_verificacion WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            System.out.println("--- DEBUG CÓDIGOS ACTUALES EN BD (ID " + idUsuario + ") ---");
            while(rs.next()) {
                System.out.println(" -> " + rs.getString("codigo") + " (Usado: " + rs.getBoolean("usado") + ")");
            }
            System.out.println("------------------------------------------------");
        } catch(Exception e) {}
    }

    // Stubs para compatibilidad con el controlador
    public boolean validarCodigo2FA(int id, String c) { return false; }
    public List<Usuario> listar() { return new ArrayList<>(); }
    public Usuario obtenerPorId(int id) { return null; }
    public boolean actualizar(Usuario u) { return false; }
    public boolean eliminar(int id) { return false; }
}