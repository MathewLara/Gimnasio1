package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Usuario;
import com.mathew.gimnasio.util.SecurityUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DE USUARIOS
 * Es el motor principal de la base de datos para la gestión de cuentas.
 * Maneja transacciones complejas, validación de códigos de correo,
 * activación de cuentas y el inicio de sesión seguro (login).
 */
public class UsuarioDAO {

    // ================================================================
    // 1. REGISTRO ATÓMICO (Usuario + Cliente + Código en UNA SOLA VEZ)
    // ================================================================

    /**
     * REGISTRO TRANSACCIONAL DE NUEVO USUARIO
     * Este método es "atómico": intenta guardar datos en TRES tablas distintas
     * (usuarios, clientes y codigos_verificacion). Si alguna de las tres falla,
     * hace un "rollback" y deshace todo para evitar datos incompletos o corruptos.
     * @param u El objeto Usuario con todos los datos del formulario.
     * @param codigoGenerado El código de 6 dígitos creado por el controlador.
     * @return true si las 3 inserciones fueron exitosas, false si hubo un error.
     */
    public boolean registrarNuevoUsuario(Usuario u, String codigoGenerado) {
        Connection conn = null;
        PreparedStatement psUsuario = null;
        PreparedStatement psCliente = null;
        PreparedStatement psCodigo = null;
        ResultSet rs = null;

        System.out.println(">>> DAO: Iniciando Transacción de Registro Completo <<<");

        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // ¡IMPORTANTE! Apagamos el guardado automático para iniciar la transacción

            // --- A. INSERTAR USUARIO (Tabla de credenciales) ---
            String sqlUsuario = "INSERT INTO usuarios (id_rol, usuario, contrasena, activo) VALUES (?, ?, ?, ?)";
            // Pedimos a la BD que nos devuelva el ID autogenerado (PK) del nuevo usuario
            psUsuario = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS);

            psUsuario.setInt(1, u.getIdRol());
            psUsuario.setString(2, u.getUsuario());
            psUsuario.setString(3, SecurityUtil.encriptar(u.getContrasena())); // Encriptación vital
            psUsuario.setBoolean(4, false); // Nace inactivo hasta validar su correo

            if (psUsuario.executeUpdate() == 0) throw new SQLException("Fallo al insertar usuario.");

            // Recuperamos el ID que la base de datos le asignó a este nuevo usuario
            rs = psUsuario.getGeneratedKeys();
            int idGenerado = 0;
            if (rs.next()) {
                idGenerado = rs.getInt(1);
                u.setIdUsuario(idGenerado);
            }

            // --- B. INSERTAR CLIENTE (Tabla de datos personales) ---
            String nombre = (u.getNombre() != null) ? u.getNombre() : "N/A";
            String apellido = (u.getApellido() != null) ? u.getApellido() : "N/A";
            String emailLimpio = u.getEmail().trim().toLowerCase();

            java.sql.Date fechaSql = null;
            if (u.getFechaNacimiento() != null && !u.getFechaNacimiento().isEmpty()) {
                try { fechaSql = java.sql.Date.valueOf(u.getFechaNacimiento()); } catch (Exception e) {}
            }

            // Usamos el idGenerado del paso A como llave foránea (FK) aquí
            String sqlCliente = "INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono, fecha_nacimiento) VALUES (?, ?, ?, ?, ?, ?)";
            psCliente = conn.prepareStatement(sqlCliente);
            psCliente.setInt(1, idGenerado);
            psCliente.setString(2, nombre);
            psCliente.setString(3, apellido);
            psCliente.setString(4, emailLimpio);
            psCliente.setString(5, u.getTelefono());
            psCliente.setDate(6, fechaSql);

            psCliente.executeUpdate();

            // --- C. INSERTAR CÓDIGO (Para verificación de correo) ---
            System.out.println(">>> DAO: Guardando código " + codigoGenerado + " para ID " + idGenerado);
            String sqlCodigo = "INSERT INTO codigos_verificacion (id_usuario, codigo, fecha_expiracion) VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '15 minutes')";
            psCodigo = conn.prepareStatement(sqlCodigo);
            psCodigo.setInt(1, idGenerado); // Misma llave foránea
            psCodigo.setString(2, codigoGenerado);
            psCodigo.executeUpdate();

            // --- CONFIRMAR TODO ---
            conn.commit(); // Si llegamos aquí sin errores, guardamos definitivamente las 3 tablas.
            System.out.println(">>> DAO: ¡Registro Completado Exitosamente! <<<");
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            // Si algo falla, hacemos ROLLBACK: se cancela la transacción completa.
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            // Cerramos todos los recursos de memoria pase lo que pase
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

    /**
     * VALIDAR CÓDIGO DE CORREO
     * Busca al usuario por su email (sin importar si es cliente o entrenador),
     * verifica si el código ingresado coincide y, de ser así, "quema" el código y activa la cuenta.
     */
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

            // A. Buscar ID (Buscamos en ambas tablas usando UNION)
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

            // B. Validar Código en la tabla de códigos
            String sqlValidar = "SELECT id_codigo, codigo FROM codigos_verificacion WHERE id_usuario = ? AND codigo = ? AND usado = FALSE";
            ps = conn.prepareStatement(sqlValidar);
            ps.setInt(1, idEncontrado);
            ps.setString(2, codigoIngresado.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("¡ÉXITO! Código coincide.");
                int idCodigo = rs.getInt("id_codigo");

                // Quemar código (marcarlo como usado para que no se recicle) y Activar usuario
                ps.close();
                ps = conn.prepareStatement("UPDATE codigos_verificacion SET usado = TRUE WHERE id_codigo = ?");
                ps.setInt(1, idCodigo);
                ps.executeUpdate();

                activarUsuario(idEncontrado); // Llamamos al método interno
                return true;
            } else {
                System.out.println("ERROR: Código incorrecto en BDD.");
                // Debugging: Imprime en consola qué códigos tiene realmente este usuario
                verCodigosDeUsuario(idEncontrado);
            }

        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if(conn!=null) conn.close(); } catch(Exception e){} }
        return false;
    }

    // ==========================================
    // 3. ACTIVAR USUARIO
    // ==========================================

    /**
     * ACTIVAR CUENTA DE USUARIO
     * Cambia el estado del usuario a 'TRUE' permitiéndole iniciar sesión.
     */
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

    /**
     * INICIAR SESIÓN
     * Permite loguearse usando el nombre de usuario, o el correo (ya sea de cliente o entrenador).
     * Cruza información de las tablas usando LEFT JOIN para traer el email correcto.
     */
    public Usuario login(String userOrEmail, String pass) {
        // Unimos las 3 tablas para buscar coincidencias en cualquiera de los campos
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
                // Verificar contraseña comparando con el hash de la BD
                if (SecurityUtil.verificar(pass, rs.getString("contrasena"))) {
                    u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setIdRol(rs.getInt("id_rol"));
                    u.setUsuario(rs.getString("usuario"));
                    u.setActivo(rs.getBoolean("activo"));

                    // Determinar de dónde sacar el email (si es cliente o entrenador)
                    String email = rs.getString("email_cliente");
                    if (email == null) email = rs.getString("email_entrenador");
                    u.setEmail(email);

                    u.setContrasena(null); // Seguridad: nunca devolvemos la contraseña al frontend
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return u;
    }
    // ==========================================
    // MÉTODO REAL PARA EL DASHBOARD DE ADMIN
    // ==========================================
    public String getAdminStatsJSON() {
        int totalClientes = 0;
        int totalEntrenadores = 0;
        double ingresos = 0.0;

        try (Connection conn = ConexionDB.getConnection()) {
            // 1. Contar clientes reales
            ResultSet rs1 = conn.prepareStatement("SELECT COUNT(*) FROM clientes").executeQuery();
            if(rs1.next()) totalClientes = rs1.getInt(1);

            // 2. Contar entrenadores reales
            ResultSet rs2 = conn.prepareStatement("SELECT COUNT(*) FROM entrenadores").executeQuery();
            if(rs2.next()) totalEntrenadores = rs2.getInt(1);

            // 3. Sumar el dinero real de la tabla pagos
            ResultSet rs3 = conn.prepareStatement("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos").executeQuery();
            if(rs3.next()) ingresos = rs3.getDouble(1);

        } catch(Exception e) {
            e.printStackTrace();
        }

        // Construimos un JSON manual rápido para no crear más archivos
        return "{\"totalClientes\": " + totalClientes + ", \"totalEntrenadores\": " + totalEntrenadores + ", \"ingresos\": " + ingresos + "}";
    }

    // ==========================================
    // GESTIÓN DE USUARIOS (PANEL ADMIN)
    // ==========================================

    // 1. Listar todos los usuarios con sus roles
    public String obtenerUsuariosParaAdminJSON() {
        StringBuilder json = new StringBuilder("[");
        // Hacemos un JOIN para traer el nombre del rol en lugar de solo el número
        String sql = "SELECT u.id_usuario, u.usuario, u.nombre, u.apellido, u.activo, r.nombre_rol " +
                "FROM usuarios u INNER JOIN roles r ON u.id_rol = r.id_rol " +
                "ORDER BY u.id_rol ASC, u.id_usuario DESC";

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
                        .append("\"rol\":\"").append(rs.getString("nombre_rol")).append("\",")
                        .append("\"activo\":").append(rs.getBoolean("activo"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    // 2. Eliminado Lógico (Cambiar estado activo/inactivo)
    public boolean cambiarEstadoUsuario(int idUsuario, boolean nuevoEstado) {
        String sql = "UPDATE usuarios SET activo = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, nuevoEstado);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }
    // 3. AGREGAR NUEVO PERSONAL / USUARIO
    public boolean agregarPersonalAdmin(Usuario u) {
        String sql = "INSERT INTO usuarios (id_rol, usuario, contrasena, activo, nombre, apellido) VALUES (?, ?, ?, true, ?, ?)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, u.getIdRol());
            ps.setString(2, u.getUsuario());
            ps.setString(3, SecurityUtil.encriptar(u.getContrasena()));
            ps.setString(4, u.getNombre());
            ps.setString(5, u.getApellido());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. EDITAR USUARIO (Y su contraseña si la escribe)
    public boolean editarPersonalAdmin(Usuario u) {
        boolean cambiaPass = (u.getContrasena() != null && !u.getContrasena().trim().isEmpty());
        String sql = cambiaPass
                ? "UPDATE usuarios SET id_rol=?, usuario=?, contrasena=?, nombre=?, apellido=? WHERE id_usuario=?"
                : "UPDATE usuarios SET id_rol=?, usuario=?, nombre=?, apellido=? WHERE id_usuario=?";

        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, u.getIdRol());
            ps.setString(2, u.getUsuario());
            if (cambiaPass) {
                ps.setString(3, SecurityUtil.encriptar(u.getContrasena()));
                ps.setString(4, u.getNombre());
                ps.setString(5, u.getApellido());
                ps.setInt(6, u.getIdUsuario());
            } else {
                ps.setString(3, u.getNombre());
                ps.setString(4, u.getApellido());
                ps.setInt(5, u.getIdUsuario());
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    // ==========================================
    // 5. MÉTODOS AUXILIARES Y STUBS
    // ==========================================

    /**
     * GUARDAR CÓDIGO DE DOBLE FACTOR
     * Útil si en un futuro implementan reenvío de códigos o autenticación 2FA al iniciar sesión.
     */
    public boolean guardarCodigo2FA(int idUsuario, String codigo) {
        String sql = "INSERT INTO codigos_verificacion (id_usuario, codigo, fecha_expiracion) VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '15 minutes')";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, codigo);
            ps.executeUpdate();
            return true;
        } catch (Exception e) { return false; }
    }

    /**
     * HERRAMIENTA DE DEBUGGING
     * Imprime en la consola del servidor todos los códigos asignados a un usuario.
     * Sirve exclusivamente para que el desarrollador vea por qué falla una verificación.
     */
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

    // -------------------------------------------------------------------------
    // STUBS (Plantillas vacías)
    // Estos métodos están definidos para no romper el UsuarioController,
    // pero deben ser implementados más adelante con sus sentencias SQL reales.
    // -------------------------------------------------------------------------
    public boolean validarCodigo2FA(int id, String c) { return false; }
    public List<Usuario> listar() { return new ArrayList<>(); }
    public Usuario obtenerPorId(int id) { return null; }
    public boolean actualizar(Usuario u) { return false; }
    public boolean eliminar(int id) { return false; }
}