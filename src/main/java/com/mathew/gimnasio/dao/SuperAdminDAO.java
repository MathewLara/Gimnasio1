/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Empresa;
import com.mathew.gimnasio.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO DEL SUPERADMINISTRADOR (SAAS)
 * Componente principal de acceso a datos para la gestión del modelo Multi-Tenant.
 * Administra la creación, configuración y monitoreo global de todas las empresas (gimnasios)
 * registradas en la plataforma, así como de sus administradores principales.
 */
public class SuperAdminDAO {

    // ==========================================
    // 1. DASHBOARD GENERAL
    // ==========================================

    /**
     * OBTENER DASHBOARD GLOBAL
     * Recupera los indicadores de rendimiento agregados (KPIs globales) de todo el sistema,
     * incluyendo el volumen total de empresas operativas, usuarios administradores e ingresos brutos.
     * Retorna: Una cadena JSON estructurada con la información estadística.
     */
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

    /**
     * OBTENER CATÁLOGO DE EMPRESAS
     * Extrae el listado completo de franquicias registradas en el sistema, incorporando
     * una subconsulta correlacionada para determinar el volumen de clientes de cada una.
     * Retorna: JSON Array detallado con la información institucional de las empresas.
     */
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

    /**
     * REGISTRAR NUEVA EMPRESA
     * Inserta una nueva entidad comercial en la base de datos, inicializando su estado como activo
     * y registrando su marca de tiempo (timestamp) de creación.
     * Parametro empresa: Objeto que encapsula los datos fiscales y de contacto de la franquicia.
     * Retorna: Verdadero si la operación de persistencia concluyó exitosamente.
     */
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

    /**
     * EDITAR EMPRESA EXISTENTE
     * Actualiza los metadatos institucionales de una franquicia previamente registrada.
     * Parametro id: Identificador único de la empresa a modificar.
     * Parametro nombre: Nueva razón social o nombre comercial.
     * Parametro ruc: Nuevo identificador fiscal.
     * Parametro telefono: Número de contacto actualizado.
     * Parametro direccion: Ubicación física de la sucursal.
     * Retorna: Verdadero si la actualización aplicó cambios en la base de datos.
     */
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

    /**
     * CAMBIAR ESTADO DE EMPRESA
     * Aplica un borrado lógico (desactivación) o reactiva una empresa completa,
     * afectando su visibilidad y operatividad general dentro de la plataforma.
     * Parametro id: Identificador de la empresa.
     * Parametro estado: Nuevo estado de operación (verdadero/falso).
     * Retorna: Confirmación de modificación en la tabla.
     */
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

    /**
     * VERIFICAR EXISTENCIA DE RUC
     * Comprueba si un Registro Único de Contribuyentes ya fue dado de alta en la plataforma,
     * garantizando la unicidad fiscal de los inquilinos (Tenants).
     * Parametro ruc: Identificador fiscal a evaluar.
     * Retorna: Verdadero en caso de existir colisión en la base de datos.
     */
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

    /**
     * VERIFICAR EXISTENCIA DE NOMBRE DE EMPRESA
     * Impide el registro de dos sucursales o franquicias operando bajo la misma
     * denominación comercial exacta en todo el ecosistema.
     * Parametro nombre: Nombre de la empresa a validar.
     * Retorna: Verdadero si el nombre ya está registrado.
     */
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

    /**
     * VERIFICAR EXISTENCIA DE TELÉFONO
     * Previene la duplicidad de información de contacto a nivel corporativo.
     * Parametro telefono: Número de contacto a evaluar.
     * Retorna: Verdadero si el teléfono ya pertenece a otra entidad.
     */
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

    /**
     * VERIFICAR EXISTENCIA DE DIRECCIÓN
     * Impide que más de una empresa sea registrada utilizando exactamente la misma
     * ubicación física.
     * Parametro direccion: Dirección postal a cotejar.
     * Retorna: Verdadero en caso de conflicto de direcciones.
     */
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

    /**
     * VERIFICAR EXISTENCIA DE CORREO ELECTRÓNICO (USUARIOS)
     * Verifica globalmente que una dirección de correo no esté asignada a ningún
     * usuario del sistema antes de proceder con nuevas creaciones de cuenta.
     * Parametro correo: Dirección de correo electrónico a validar.
     * Retorna: Verdadero si el correo electrónico ya está en uso.
     */
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

    /**
     * OBTENER LISTADO DE ADMINISTRADORES
     * Consulta relacional para extraer los perfiles de todos los usuarios categorizados
     * con el rol de Administrador (Role ID 1), incluyendo el nombre de la empresa a la que gestionan.
     * Retorna: JSON Array detallado con la información de los administradores locales.
     */
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

    /**
     * GUARDAR NUEVO ADMINISTRADOR
     * Crea un perfil de usuario con nivel de acceso máximo para una sucursal, aplicando
     * métodos de criptografía (SecurityUtil) a sus credenciales de acceso antes de persistirlas.
     * Parametro idEmpresa: Franquicia sobre la cual tendrá autoridad operativa.
     * Parametro nombre: Nombres del administrador.
     * Parametro apellido: Apellidos del administrador.
     * Parametro usuario: Alias de inicio de sesión (username).
     * Parametro contrasena: Clave de acceso en texto plano (será encriptada).
     * Retorna: Verdadero si la cuenta se generó correctamente en la base de datos.
     */
    public boolean guardarAdmin(int idEmpresa, String nombre, String apellido, String usuario, String contrasena) {
        String sql = "INSERT INTO usuarios (id_rol, id_empresa, usuario, contrasena, nombre, apellido, activo) VALUES (1, ?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEmpresa);
            ps.setString(2, usuario);
            ps.setString(3, SecurityUtil.encriptar(contrasena)); // Se aplica seguridad criptográfica
            ps.setString(4, nombre);
            ps.setString(5, apellido);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    /**
     * EDITAR ADMINISTRADOR EXISTENTE
     * Actualiza la información personal de un administrador, evaluando de forma dinámica
     * si la consulta debe aplicar una nueva encriptación en caso de detectarse un cambio de contraseña.
     * Parametro idUsuario: Identificador de la cuenta a alterar.
     * Parametro idEmpresa: Sucursal asignada.
     * Parametro nombre: Nuevos nombres.
     * Parametro apellido: Nuevos apellidos.
     * Parametro usuario: Nuevo alias de inicio de sesión.
     * Parametro contrasena: Nueva clave (si aplica, caso contrario mantener actual).
     * Retorna: Estado de la transacción de actualización.
     */
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

    /**
     * CAMBIAR ESTADO DE ADMINISTRADOR
     * Suspende o rehabilita el acceso de un gerente/administrador local a la plataforma,
     * actualizando su estado de conexión permitida.
     * Parametro id: Identificador de la cuenta.
     * Parametro estado: Booleano que define si la cuenta está habilitada.
     * Retorna: Confirmación del cambio de privilegios en el sistema.
     */
    public boolean cambiarEstadoAdmin(int id, boolean estado) {
        String sql = "UPDATE usuarios SET activo=? WHERE id_usuario=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }
}