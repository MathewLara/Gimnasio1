package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import com.mathew.gimnasio.modelos.AsignarAlumnoDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * DATA ACCESS OBJECT (DAO) DEL ENTRENADOR
 * Gestiona toda la lógica de negocio del personal técnico: creación de rutinas,
 * vinculación/desvinculación de alumnos y clonación de planes de entrenamiento.
 */
public class EntrenadorDAO {

    /**
     * MÉTODO INTERNO: OBTENER ID DEL CLIENTE FANTASMA
     * Resuelve un problema de diseño de Base de Datos: La tabla 'rutinas' exige
     * un 'id_cliente' obligatorio (NOT NULL). Para guardar "Plantillas de Rutina"
     * que aún no tienen dueño, el sistema crea dinámicamente un cliente "Fantasma"
     * (Plantilla Sistema) y le asigna estas rutinas.
     * @param conn Conexión activa.
     * @return ID numérico del cliente fantasma.
     */
    // ==================================================
    // TRUCO MAESTRO: Crear un "Cliente Fantasma" para las plantillas
    // Así evitamos que la BD explote por reglas de "NOT NULL" o "Foreign Keys"
    // ==================================================
    private int obtenerIdPlantilla(Connection conn) throws Exception {
        // 1. Buscamos si ya existe el cliente fantasma "Plantilla"
        PreparedStatement ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE nombre = 'Plantilla' AND apellido = 'Sistema'");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);

        // 2. Si no existe, CREAMOS EL USUARIO PRIMERO (Obligatorio por tu llave foránea)
        // Usamos id_rol = 3 que en tu BD corresponde a 'Cliente'
        ps = conn.prepareStatement("INSERT INTO usuarios (id_rol, usuario, contrasena, nombre, apellido, activo) VALUES (3, 'plantilla_sys', '12345', 'Plantilla', 'Sistema', false) RETURNING id_usuario");
        rs = ps.executeQuery();
        int idUsr = 0;
        if (rs.next()) idUsr = rs.getInt(1);

        // 3. AHORA SÍ, creamos el cliente usando el id_usuario real que acabamos de generar
        ps = conn.prepareStatement("INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono) VALUES (?, 'Plantilla', 'Sistema', 'plantilla@sistema.com', '0000000000') RETURNING id_cliente");
        ps.setInt(1, idUsr);
        rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);

        return 0;
    }

    /**
     * OBTENER TELEMETRÍA DEL DASHBOARD DEL ENTRENADOR
     * @param idUsuario ID de autenticación del entrenador logueado.
     * @return DTO consolidado con KPIs, alumnos vinculados y biblioteca de rutinas.
     */
    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario) {
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn);

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

            // Las rutinas de la biblioteca son las del cliente Fantasma
            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente = ?");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            if (rs.next()) dto.rutinasCreadas = rs.getInt(1);

            // Los alumnos son todos los que NO son el fantasma
            ps = conn.prepareStatement("SELECT COUNT(DISTINCT id_cliente) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente != ?");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            if (rs.next()) dto.totalAlumnos = rs.getInt(1);

            // Obtener lista de alumnos con su estado de rutina del día actual
            dto.listaAlumnos = new ArrayList<>();
            String sqlAlumnos = "SELECT DISTINCT c.id_usuario, c.nombre || ' ' || c.apellido as n, " +
                    "COALESCE(m.nombre, 'Sin Plan') as plan, r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as termino " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE r.id_entrenador = ? AND r.activa = TRUE AND r.id_cliente != ?";

            ps = conn.prepareStatement(sqlAlumnos);
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            while (rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_usuario"), rs.getString("n"), rs.getString("plan"), rs.getString("nombre_rutina"), yaTermino
                ));
            }

            // Obtener biblioteca de rutinas (Plantillas) y los ejercicios que contienen
            dto.listaRutinas = new ArrayList<>();
            ps = conn.prepareStatement("SELECT id_rutina, nombre_rutina, activa, id_cliente FROM rutinas WHERE id_entrenador = ? AND id_cliente = ? ORDER BY id_rutina DESC");
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

    /**
     * CREAR NUEVA RUTINA (PLANTILLA)
     * Utiliza BATCH PROCESSING (ps.addBatch/executeBatch) para insertar
     * múltiples ejercicios de forma optimizada y en una sola transacción ACID.
     */
    public boolean crearRutina(int idUsuarioEntrenador, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Inicia Transacción

            // Magia: Obtenemos el ID del fantasma
            int idPlantilla = obtenerIdPlantilla(conn);

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
            ps.setString(3, datos.getNombreRutina());
            rs = ps.executeQuery();

            int idRutina = 0;
            if (rs.next()) idRutina = rs.getInt(1);

            // Inserción masiva de ejercicios
            if (datos.getIdsEjercicios() != null) {
                String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
                ps = conn.prepareStatement(sqlDetalle);
                for (Integer idEjercicio : datos.getIdsEjercicios()) {
                    ps.setInt(1, idRutina);
                    ps.setInt(2, idEjercicio);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit(); // Cierra Transacción
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {} // Revierte si falla
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    /**
     * ACTUALIZAR RUTINA EXISTENTE
     * Elimina los ejercicios previos (DELETE) y vuelve a insertar la nueva selección (INSERT BATCH).
     */
    public boolean actualizarRutina(int idRutina, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);
            int idPlantilla = obtenerIdPlantilla(conn);

            String sqlUpdate = "UPDATE rutinas SET nombre_rutina = ?, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, datos.getNombreRutina());
            ps.setInt(2, idPlantilla);
            ps.setInt(3, idRutina);
            ps.executeUpdate();

            ps = conn.prepareStatement("DELETE FROM detalle_rutinas WHERE id_rutina = ?");
            ps.setInt(1, idRutina);
            ps.executeUpdate();

            if (datos.getIdsEjercicios() != null) {
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
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    /**
     * SOFT DELETE: DESACTIVAR RUTINA (Papelera)
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
     * RESTAURAR RUTINA DESDE PAPELERA
     */
    public boolean reactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn);
            String sql = "UPDATE rutinas SET activa = TRUE, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idPlantilla);
            ps.setInt(2, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * OBTENER AGENDA DEL DÍA (Seguimiento de Asistencia)
     */
    public java.util.List<EntrenadorDashboardDTO.AlumnoResumen> obtenerAgendaHoy(int idUsuarioEntrenador) {
        java.util.List<EntrenadorDashboardDTO.AlumnoResumen> agenda = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn);
            String sql = "SELECT c.id_usuario, c.nombre || ' ' || c.apellido as n, r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as completo " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE e.id_usuario = ? AND r.activa = TRUE AND c.id_cliente != ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuarioEntrenador);
            ps.setInt(2, idPlantilla);
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
     * VINCULAR ALUMNO Y CLONAR RUTINA
     * Este es un proceso pesado. Verifica si el usuario existe como 'cliente'.
     * Si no existe (ej. recién registrado en web), lo inserta en la tabla clientes.
     * Luego, copia la plantilla de rutina elegida y la asigna al nuevo alumno.
     */
    public boolean vincularAlumno(int idUsuarioEntrenador, AsignarAlumnoDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Operación Crítica (Transacción)

            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt(1);
            else return false;

            // SOLUCIÓN A MICAELA: Traemos los datos de la tabla usuarios y lo registramos de verdad
            int realIdCliente = 0;
            ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?");
            ps.setInt(1, datos.getIdCliente()); // Es el id_usuario
            rs = ps.executeQuery();

            if (rs.next()) {
                realIdCliente = rs.getInt(1);
            } else {
                // CORRECCIÓN: La tabla usuarios no tiene email ni teléfono, solo pedimos nombre y apellido
                ps = conn.prepareStatement("SELECT nombre, apellido FROM usuarios WHERE id_usuario = ?");
                ps.setInt(1, datos.getIdCliente());
                ResultSet rsUsr = ps.executeQuery();

                if(rsUsr.next()){
                    String n = rsUsr.getString("nombre");
                    String a = rsUsr.getString("apellido");

                    // Insertamos al cliente usando 'email' dummy y valores por defecto para evitar errores NOT NULL
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
                    return false; // El usuario ni siquiera existe en la BD
                }
            }
            // Desactivar rutinas viejas asignadas por este profesor
            ps = conn.prepareStatement("UPDATE rutinas SET id_entrenador = NULL WHERE id_cliente = ? AND id_entrenador = ?");
            ps.setInt(1, realIdCliente);
            ps.setInt(2, idEntrenador);
            ps.executeUpdate();

            // LÓGICA DE CLONACIÓN DE RUTINA (Deep Copy)
            if (datos.getIdRutinaAsignada() > 0) {
                String nombreOriginal = "Rutina Personalizada";
                ps = conn.prepareStatement("SELECT nombre_rutina FROM rutinas WHERE id_rutina = ?");
                ps.setInt(1, datos.getIdRutinaAsignada());
                rs = ps.executeQuery();
                if(rs.next()) nombreOriginal = rs.getString(1);

                // Inserta la cabecera copiada
                ps = conn.prepareStatement("INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina");
                ps.setInt(1, realIdCliente);
                ps.setInt(2, idEntrenador);
                ps.setString(3, nombreOriginal + " (Asignada)");
                rs = ps.executeQuery();

                if(rs.next()) {
                    int nuevaId = rs.getInt(1);
                    // Copia masiva de detalles desde la base de datos (INSERT ... SELECT)
                    ps = conn.prepareStatement("INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) SELECT ?, id_ejercicio, series, repeticiones FROM detalle_rutinas WHERE id_rutina = ?");
                    ps.setInt(1, nuevaId);
                    ps.setInt(2, datos.getIdRutinaAsignada());
                    ps.executeUpdate();
                }
            } else {
                // Si el profe no eligió plantilla, crea una rutina genérica vacía
                ps = conn.prepareStatement("INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, 'Plan de Entrenamiento', CURRENT_DATE, TRUE)");
                ps.setInt(1, realIdCliente);
                ps.setInt(2, idEntrenador);
                ps.executeUpdate();
            }

            // Limpia el historial para que el alumno pueda entrenar "Hoy" con la nueva rutina
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
     * Remueve al entrenador asignado pero no elimina al cliente del sistema.
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

            // Desactiva solo las rutinas que ESTE entrenador le asignó a ESTE cliente
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
}