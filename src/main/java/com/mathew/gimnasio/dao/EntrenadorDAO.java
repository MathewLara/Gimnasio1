/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import com.mathew.gimnasio.modelos.AsignarAlumnoDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * DAO DE ENTRENADORES
 * Componente encargado de gestionar toda la persistencia de datos relacionada con el módulo
 * de entrenadores. Interactúa con las tablas de rutinas, ejercicios, alumnos asignados y
 * consolida la información para el dashboard del personal técnico.
 */
public class EntrenadorDAO {

    // ==========================================
    // MÉTODO INTERNO DE APOYO
    // ==========================================

    /**
     * OBTENER ID DE PLANTILLA
     * Método interno auxiliar que recupera o genera un cliente "plantilla" a nivel de sistema.
     * Este registro comodín permite a los entrenadores crear y guardar rutinas en su biblioteca
     * personal sin necesidad de asignarlas inmediatamente a un cliente real.
     * Parametro conn: Conexión activa a la base de datos.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: El identificador único del cliente plantilla.
     */
    private int obtenerIdPlantilla(Connection conn, int idEmpresa) {
        try {
            String queryBusqueda = "SELECT c.id_cliente FROM clientes c JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE u.usuario = ?";
            PreparedStatement ps = conn.prepareStatement(queryBusqueda);
            ps.setString(1, "plantilla_sys_" + idEmpresa);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

            ps = conn.prepareStatement("INSERT INTO usuarios (id_rol, usuario, contrasena, nombre, apellido, activo, id_empresa) VALUES (3, ?, '12345', 'Plantilla', 'Sistema', false, ?) RETURNING id_usuario");
            ps.setString(1, "plantilla_sys_" + idEmpresa);
            ps.setInt(2, idEmpresa);
            rs = ps.executeQuery();
            int idUsr = 0;
            if (rs.next()) idUsr = rs.getInt(1);

            ps = conn.prepareStatement("INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono) VALUES (?, 'Plantilla', 'Sistema', ?, '0000000000') RETURNING id_cliente");
            ps.setInt(1, idUsr);
            ps.setString(2, "plantilla_" + idEmpresa + "@sistema.com");
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            System.out.println("Nota interna: Plantilla existente o conflicto atrapado.");
        }
        return 0;
    }

    // ==========================================
    // REGLAS DE NEGOCIO Y VALIDACIONES
    // ==========================================

    /**
     * VALIDAR DUPLICIDAD DE NOMBRE DE RUTINA
     * Verifica que un entrenador no asigne el mismo nombre a dos rutinas diferentes
     * dentro de su biblioteca personal para evitar conflictos de identificación.
     * Parametro nombre: Nombre de la rutina a validar.
     * Parametro idUsuarioEntrenador: Identificador del entrenador creador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro idRutinaExcluida: Identificador a omitir en la validación (útil durante la edición).
     * Retorna: Verdadero si el nombre ya está en uso.
     */
    public boolean existeNombreRutina(String nombre, int idUsuarioEntrenador, int idEmpresa, int idRutinaExcluida) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa);
            int idEntrenador = 0;
            PreparedStatement ps1 = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps1.setInt(1, idUsuarioEntrenador);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) idEntrenador = rs1.getInt(1);
            else return false;

            String sql = "SELECT COUNT(*) FROM rutinas WHERE LOWER(nombre_rutina) = LOWER(?) AND id_entrenador = ? AND id_cliente = ? AND id_rutina != ?";
            PreparedStatement ps2 = conn.prepareStatement(sql);
            ps2.setString(1, nombre.trim());
            ps2.setInt(2, idEntrenador);
            ps2.setInt(3, idPlantilla);
            ps2.setInt(4, idRutinaExcluida);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) return rs2.getInt(1) > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    /**
     * VALIDAR DUPLICIDAD DE NOMBRE DE EJERCICIO
     * Previene la inserción de ejercicios con nombres idénticos en el catálogo global.
     * Parametro nombre: Nombre del ejercicio a evaluar.
     * Parametro idExcluido: ID del ejercicio a ignorar (para procesos de edición).
     * Retorna: Verdadero si ya existe un ejercicio con ese nombre exacto.
     */
    public boolean existeNombreEjercicio(String nombre, int idExcluido) {
        String sql = "SELECT COUNT(*) FROM ejercicios WHERE LOWER(nombre_ejercicio) = LOWER(?) AND id_ejercicio != ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ==========================================
    // DASHBOARD ENTRENADOR
    // ==========================================

    /**
     * OBTENER TELEMETRÍA PARA DASHBOARD DE ENTRENADOR
     * Ejecuta múltiples consultas para consolidar el perfil del entrenador, su total
     * de alumnos activos, rutinas creadas y un resumen de las asignaciones vigentes.
     * Parametro idUsuario: Identificador del entrenador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Objeto DTO con las estadísticas estructuradas.
     */
    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario, int idEmpresa) {
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa);

            String sqlEnt = "SELECT id_entrenador, nombre || ' ' || apellido as n, especialidad FROM entrenadores WHERE id_usuario = ?";
            PreparedStatement ps = conn.prepareStatement(sqlEnt);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            int idEntrenador = 0;
            if (rs.next()) {
                idEntrenador = rs.getInt("id_entrenador");
                dto.nombre = rs.getString("n");
                dto.especialidad = rs.getString("especialidad");
            }

            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND (id_cliente = ? OR id_cliente = 0 OR id_cliente IS NULL)");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            if (rs.next()) dto.rutinasCreadas = rs.getInt(1);

            ps = conn.prepareStatement("SELECT COUNT(DISTINCT id_cliente) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente != ? AND id_cliente != 0");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            if (rs.next()) dto.totalAlumnos = rs.getInt(1);

            dto.listaAlumnos = new ArrayList<>();
            String sqlAlumnos = "SELECT c.id_usuario, c.nombre || ' ' || c.apellido as n, " +
                    "COALESCE(MAX(m.nombre), 'Sin Plan') as plan, " +
                    "STRING_AGG(DISTINCT r.nombre_rutina, ' | ') as nombre_rutina, " +
                    "CASE WHEN MAX(h.id_historial) IS NOT NULL THEN 'SI' ELSE 'NO' END as termino " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE r.id_entrenador = ? AND r.activa = TRUE AND r.id_cliente != ? AND r.id_cliente != 0 AND u.id_empresa = ? " +
                    "GROUP BY c.id_usuario, c.nombre, c.apellido";

            ps = conn.prepareStatement(sqlAlumnos);
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            ps.setInt(3, idEmpresa);
            rs = ps.executeQuery();

            while (rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_usuario"), rs.getString("n"), rs.getString("plan"), rs.getString("nombre_rutina"), yaTermino
                ));
            }

            dto.listaRutinas = new ArrayList<>();
            ps = conn.prepareStatement("SELECT id_rutina, nombre_rutina, activa, id_cliente FROM rutinas WHERE id_entrenador = ? AND (id_cliente = ? OR id_cliente = 0 OR id_cliente IS NULL) ORDER BY id_rutina DESC");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            while (rs.next()) {
                EntrenadorDashboardDTO.RutinaItem item = new EntrenadorDashboardDTO.RutinaItem(
                        rs.getInt("id_rutina"), rs.getString("nombre_rutina"), rs.getBoolean("activa"), rs.getInt("id_cliente")
                );
                PreparedStatement ps2 = conn.prepareStatement("SELECT id_ejercicio FROM detalle_rutinas WHERE id_rutina = ?");
                ps2.setInt(1, item.id);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) item.idsEjercicios.add(rs2.getInt(1));
                dto.listaRutinas.add(item);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }

    // ==========================================
    // GESTIÓN DE RUTINAS
    // ==========================================

    /**
     * CREAR RUTINA DE ENTRENAMIENTO
     * Maneja una transacción en base de datos para insertar la cabecera de una rutina
     * y, mediante la ejecución por lotes (batch processing), insertar todos los ejercicios
     * asociados asegurando la integridad referencial.
     * Parametro idUsuarioEntrenador: Identificador del autor de la rutina.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro datos: DTO con la estructura de la nueva rutina.
     * Retorna: Verdadero si la transacción completa (cabecera + detalle) finaliza con éxito.
     */
    public boolean crearRutina(int idUsuarioEntrenador, int idEmpresa, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Inicia transacción explícita

            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa);

            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt("id_entrenador");
            else return false;

            String sqlRutina = "INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina";
            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, idPlantilla);
            ps.setInt(2, idEntrenador);
            ps.setString(3, datos.getNombreRutina().trim());
            rs = ps.executeQuery();

            int idRutina = 0;
            if (rs.next()) idRutina = rs.getInt(1);

            // Inserción en lote (Batch) de los ejercicios seleccionados
            if (datos.getIdsEjercicios() != null && !datos.getIdsEjercicios().isEmpty()) {
                String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
                ps = conn.prepareStatement(sqlDetalle);
                for (Integer idEjercicio : datos.getIdsEjercicios()) {
                    ps.setInt(1, idRutina);
                    ps.setInt(2, idEjercicio);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit(); // Confirma la transacción
            return true;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    /**
     * MODIFICAR RUTINA EXISTENTE
     * Actualiza el encabezado de la rutina y regenera el detalle de los ejercicios
     * mediante la eliminación y posterior inserción en lote (estrategia delete-and-insert)
     * bajo una transacción controlada.
     * Parametro idRutina: Identificador de la rutina a modificar.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro datos: Nueva estructura y contenido de la rutina.
     * Retorna: Verdadero si la modificación estructural tiene éxito.
     */
    public boolean modificarRutina(int idRutina, int idEmpresa, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa);

            String sqlUpdate = "UPDATE rutinas SET nombre_rutina = ?, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, datos.getNombreRutina().trim());
            ps.setInt(2, idPlantilla);
            ps.setInt(3, idRutina);
            ps.executeUpdate();

            // Limpieza previa del detalle para asegurar consistencia
            ps = conn.prepareStatement("DELETE FROM detalle_rutinas WHERE id_rutina = ?");
            ps.setInt(1, idRutina);
            ps.executeUpdate();

            // Inserción del nuevo set de ejercicios
            if (datos.getIdsEjercicios() != null && !datos.getIdsEjercicios().isEmpty()) {
                String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
                ps = conn.prepareStatement(sqlDetalle);
                for (Integer idEjercicio : datos.getIdsEjercicios()) {
                    ps.setInt(1, idRutina);
                    ps.setInt(2, idEjercicio);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    /**
     * DESACTIVAR RUTINA (ELIMINADO LÓGICO)
     * Deshabilita la rutina en el sistema sin eliminarla físicamente, preservando
     * la integridad histórica de los registros y entrenamientos pasados.
     * Parametro idRutina: Identificador de la rutina.
     * Retorna: Estado de la actualización en base de datos.
     */
    public boolean desactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "UPDATE rutinas SET activa = FALSE WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * REACTIVAR RUTINA
     * Restaura una rutina enviada a la papelera, asignándola de nuevo a la plantilla
     * predeterminada del sistema.
     * Parametro idRutina: Identificador de la rutina.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Verdadero si se logró la reactivación.
     */
    public boolean reactivarRutina(int idRutina, int idEmpresa) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa);
            String sql = "UPDATE rutinas SET activa = TRUE, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idPlantilla);
            ps.setInt(2, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ==========================================
    // AGENDA Y ALUMNOS
    // ==========================================

    /**
     * OBTENER AGENDA DEL DÍA
     * Recupera el listado de alumnos asignados al entrenador, indicando qué rutina
     * les corresponde y si ya han culminado su entrenamiento en la fecha actual.
     * Parametro idUsuarioEntrenador: Identificador del entrenador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Colección de resúmenes de alumnos para el panel del día.
     */
    public java.util.List<EntrenadorDashboardDTO.AlumnoResumen> obtenerAgendaHoy(int idUsuarioEntrenador, int idEmpresa) {
        java.util.List<EntrenadorDashboardDTO.AlumnoResumen> agenda = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa);
            String sql = "SELECT c.id_usuario, c.nombre || ' ' || c.apellido as n, " +
                    "STRING_AGG(DISTINCT r.nombre_rutina, ' | ') as nombre_rutina, " +
                    "CASE WHEN MAX(h.id_historial) IS NOT NULL THEN 'SI' ELSE 'NO' END as completo " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                    "JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE e.id_usuario = ? AND r.activa = TRUE AND c.id_cliente != ? AND c.id_cliente != 0 AND u.id_empresa = ? " +
                    "GROUP BY c.id_usuario, c.nombre, c.apellido";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuarioEntrenador);
            ps.setInt(2, idPlantilla);
            ps.setInt(3, idEmpresa);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                boolean termino = "SI".equals(rs.getString("completo"));
                agenda.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_usuario"), rs.getString("n"), "Hoy", rs.getString("nombre_rutina"), termino
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return agenda;
    }

    /**
     * VINCULAR ALUMNO A RUTINA
     * Ejecuta una transacción compleja que valida límites máximos de rutinas activas,
     * asegura que el cliente exista en la base operativa y procede a clonar
     * la estructura de entrenamiento de la biblioteca hacia el perfil del alumno.
     * Parametro idUsuarioEntrenador: Identificador del entrenador a cargo.
     * Parametro datos: Información de asignación enviada desde el controlador.
     * Retorna: Verdadero si se establece el vínculo sin exceder límites ni causar colisiones.
     */
    public boolean vincularAlumno(int idUsuarioEntrenador, AsignarAlumnoDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt(1);
            else return false;

            int realIdCliente = 0;
            ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?");
            ps.setInt(1, datos.getIdCliente());
            rs = ps.executeQuery();

            if (rs.next()) {
                realIdCliente = rs.getInt(1);
            } else {
                // Procedimiento de contingencia: Inserción del perfil cliente si solo existe como usuario
                ps = conn.prepareStatement("SELECT nombre, apellido FROM usuarios WHERE id_usuario = ?");
                ps.setInt(1, datos.getIdCliente());
                ResultSet rsUsr = ps.executeQuery();

                if(rsUsr.next()){
                    String n = rsUsr.getString("nombre");
                    String a = rsUsr.getString("apellido");

                    ps = conn.prepareStatement("INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono) VALUES (?, ?, ?, ?, ?) RETURNING id_cliente");
                    ps.setInt(1, datos.getIdCliente());
                    ps.setString(2, n != null ? n : "Alumno");
                    ps.setString(3, a != null ? a : "Nuevo");
                    ps.setString(4, "sin_email_" + datos.getIdCliente() + "@gym.com");
                    ps.setString(5, "0000000000");

                    ResultSet rsIns = ps.executeQuery();
                    if(rsIns.next()) realIdCliente = rsIns.getInt(1);
                    else return false;
                } else {
                    return false;
                }
            }

            // Control de límite de asignaciones por seguridad operativa
            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_cliente = ? AND id_entrenador = ? AND activa = TRUE");
            ps.setInt(1, realIdCliente);
            ps.setInt(2, idEntrenador);
            rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) >= 10) {
                return false;
            }

            // Lógica de clonado de la rutina seleccionada al perfil del cliente
            if (datos.getIdRutinaAsignada() > 0) {
                String nombreOriginal = "Rutina Personalizada";
                ps = conn.prepareStatement("SELECT nombre_rutina FROM rutinas WHERE id_rutina = ?");
                ps.setInt(1, datos.getIdRutinaAsignada());
                rs = ps.executeQuery();
                if(rs.next()) nombreOriginal = rs.getString(1);

                ps = conn.prepareStatement("INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina");
                ps.setInt(1, realIdCliente);
                ps.setInt(2, idEntrenador);
                ps.setString(3, nombreOriginal + " (Asignada)");
                rs = ps.executeQuery();

                if(rs.next()) {
                    int nuevaId = rs.getInt(1);
                    ps = conn.prepareStatement("INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) SELECT ?, id_ejercicio, series, repeticiones FROM detalle_rutinas WHERE id_rutina = ?");
                    ps.setInt(1, nuevaId);
                    ps.setInt(2, datos.getIdRutinaAsignada());
                    ps.executeUpdate();
                }
            } else {
                ps = conn.prepareStatement("INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, 'Plan de Entrenamiento', CURRENT_DATE, TRUE)");
                ps.setInt(1, realIdCliente);
                ps.setInt(2, idEntrenador);
                ps.executeUpdate();
            }

            // Reseteo de historial diario para forzar nueva ejecución por el alumno
            ps = conn.prepareStatement("DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE");
            ps.setInt(1, realIdCliente);
            ps.executeUpdate();

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    /**
     * DESVINCULAR ALUMNO
     * Retira la tutoría de un entrenador sobre un alumno, desactivando las rutinas
     * asignadas en lugar de borrarlas, protegiendo así el historial del cliente.
     * Parametro idUsuarioEntrenador: Identificador del entrenador actual.
     * Parametro idUsuarioCliente: Identificador del cliente.
     * Retorna: Confirmación de desvinculación operativa.
     */
    public boolean desvincularAlumno(int idUsuarioEntrenador, int idUsuarioCliente) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt(1);

            int realIdCliente = 0;
            ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioCliente);
            rs = ps.executeQuery();
            if (rs.next()) realIdCliente = rs.getInt(1);
            else return false;

            ps = conn.prepareStatement("UPDATE rutinas SET activa = FALSE WHERE id_cliente = ? AND id_entrenador = ?");
            ps.setInt(1, realIdCliente);
            ps.setInt(2, idEntrenador);
            ps.executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // MÓDULO DE EJERCICIOS
    // ==========================================

    /**
     * OBTENER CATÁLOGO DE EJERCICIOS (JSON)
     * Realiza una extracción total de la base de ejercicios disponibles,
     * construyendo la respuesta JSON desde la misma capa DAO.
     * Retorna: Cadena String que representa el JSON Array con el catálogo de ejercicios.
     */
    public String obtenerEjerciciosJSON() {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT id_ejercicio, nombre_ejercicio, grupo_muscular, activo FROM ejercicios ORDER BY id_ejercicio ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id_ejercicio")).append(",")
                        .append("\"nombre\":\"").append(rs.getString("nombre_ejercicio")).append("\",")
                        .append("\"grupo\":\"").append(rs.getString("grupo_muscular")).append("\",")
                        .append("\"activo\":").append(rs.getBoolean("activo"))
                        .append("}");
                first = false;
            }
        } catch (Exception e) { e.printStackTrace(); }
        json.append("]");
        return json.toString();
    }

    /**
     * GUARDAR NUEVO EJERCICIO
     * Inserta un nuevo elemento en la tabla maestra de ejercicios.
     * Parametro nombre: Denominación del ejercicio (ej. Press de Banca).
     * Parametro grupo: Grupo muscular principal implicado.
     * Retorna: Verdadero en caso de inserción exitosa en base de datos.
     */
    public boolean guardarEjercicio(String nombre, String grupo) {
        String sql = "INSERT INTO ejercicios (nombre_ejercicio, grupo_muscular, activo) VALUES (?, ?, TRUE)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, grupo.trim());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * EDITAR EJERCICIO EXISTENTE
     * Actualiza la información técnica de un ejercicio ya registrado.
     * Parametro id: Identificador único del ejercicio.
     * Parametro nombre: Nueva denominación técnica a aplicar.
     * Parametro grupo: Nueva clasificación muscular.
     * Retorna: Estado de la actualización.
     */
    public boolean editarEjercicio(int id, String nombre, String grupo) {
        String sql = "UPDATE ejercicios SET nombre_ejercicio=?, grupo_muscular=? WHERE id_ejercicio=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, grupo.trim());
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * CAMBIAR ESTADO DE EJERCICIO
     * Ejecuta una desactivación lógica (soft-delete) o rehabilitación
     * sobre un ejercicio específico para controlar su visibilidad en el sistema.
     * Parametro id: Identificador del ejercicio a afectar.
     * Parametro estado: Valor booleano que definirá el nuevo estado.
     * Retorna: Confirmación del cambio a nivel de persistencia.
     */
    public boolean cambiarEstadoEjercicio(int id, boolean estado) {
        String sql = "UPDATE ejercicios SET activo=? WHERE id_ejercicio=?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}