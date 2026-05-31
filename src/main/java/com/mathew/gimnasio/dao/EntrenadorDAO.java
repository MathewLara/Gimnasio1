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
     * CORRECCIÓN: Se inyectó idEmpresa para aislar las plantillas y evitar el Error 500
     */
    private int obtenerIdPlantilla(Connection conn, int idEmpresa) throws Exception {
        String queryBusqueda = "SELECT c.id_cliente FROM clientes c JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE c.nombre = 'Plantilla' AND c.apellido = 'Sistema' AND u.id_empresa = ?";
        PreparedStatement ps = conn.prepareStatement(queryBusqueda);
        ps.setInt(1, idEmpresa);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);

        // Se agregó id_empresa al INSERT y un usuario único
        ps = conn.prepareStatement("INSERT INTO usuarios (id_rol, usuario, contrasena, nombre, apellido, activo, id_empresa) VALUES (3, ?, '12345', 'Plantilla', 'Sistema', false, ?) RETURNING id_usuario");
        ps.setString(1, "plantilla_sys_" + idEmpresa);
        ps.setInt(2, idEmpresa);
        rs = ps.executeQuery();
        int idUsr = 0;
        if (rs.next()) idUsr = rs.getInt(1);

        // Se genera un email único para la plantilla de la empresa
        ps = conn.prepareStatement("INSERT INTO clientes (id_usuario, nombre, apellido, email, telefono) VALUES (?, 'Plantilla', 'Sistema', ?, '0000000000') RETURNING id_cliente");
        ps.setInt(1, idUsr);
        ps.setString(2, "plantilla_" + idEmpresa + "@sistema.com");
        rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);

        return 0;
    }

    /**
     * OBTENER TELEMETRÍA DEL DASHBOARD DEL ENTRENADOR (AISLADO POR EMPRESA)
     */
    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario, int idEmpresa) { // <-- AQUÍ YA RECIBE LOS 2 PARÁMETROS
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa); // <-- CORRECCIÓN

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

            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente = ?");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            if (rs.next()) dto.rutinasCreadas = rs.getInt(1);

            ps = conn.prepareStatement("SELECT COUNT(DISTINCT id_cliente) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente != ?");
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            rs = ps.executeQuery();
            if (rs.next()) dto.totalAlumnos = rs.getInt(1);

            dto.listaAlumnos = new ArrayList<>();
            // --- CAMBIO: Agrupamos las múltiples rutinas en una sola fila ---
            String sqlAlumnos = "SELECT c.id_cliente, c.nombre || ' ' || c.apellido as n, " +
                    "COALESCE(MAX(m.nombre), 'Sin Plan') as plan, " +
                    "STRING_AGG(DISTINCT r.nombre_rutina, ' | ') as nombre_rutina, " +
                    "CASE WHEN MAX(h.id_historial) IS NOT NULL THEN 'SI' ELSE 'NO' END as termino " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE r.id_entrenador = ? AND r.activa = TRUE " +
                    "GROUP BY c.id_cliente, c.nombre, c.apellido";

            ps = conn.prepareStatement(sqlAlumnos);
            ps.setInt(1, idEntrenador);
            ps.setInt(2, idPlantilla);
            ps.setInt(3, idEmpresa); // <-- SE INYECTA LA EMPRESA
            rs = ps.executeQuery();

            while (rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_usuario"), rs.getString("n"), rs.getString("plan"), rs.getString("nombre_rutina"), yaTermino
                ));
            }

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

    // CORRECCIÓN: Se añadió idEmpresa
    public boolean crearRutina(int idUsuarioEntrenador, int idEmpresa, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa); // <-- CORRECCIÓN

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

    // CORRECCIÓN: Se añadió idEmpresa
    public boolean modificarRutina(int idRutina, int idEmpresa, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa); // <-- CORRECCIÓN

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

    public boolean desactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "UPDATE rutinas SET activa = FALSE WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // CORRECCIÓN: Se añadió idEmpresa
    public boolean reactivarRutina(int idRutina, int idEmpresa) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa); // <-- CORRECCIÓN
            String sql = "UPDATE rutinas SET activa = TRUE, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idPlantilla);
            ps.setInt(2, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * OBTENER AGENDA DEL DÍA (AISLADO POR EMPRESA)
     */
    public java.util.List<EntrenadorDashboardDTO.AlumnoResumen> obtenerAgendaHoy(int idUsuarioEntrenador, int idEmpresa) { // <-- AQUÍ YA RECIBE LOS 2 PARÁMETROS
        java.util.List<EntrenadorDashboardDTO.AlumnoResumen> agenda = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection()) {
            int idPlantilla = obtenerIdPlantilla(conn, idEmpresa); // <-- CORRECCIÓN

            String sql = "SELECT c.id_usuario, c.nombre || ' ' || c.apellido as n, r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as completo " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                    "JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE e.id_usuario = ? AND r.activa = TRUE AND c.id_cliente != ? AND u.id_empresa = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuarioEntrenador);
            ps.setInt(2, idPlantilla);
            ps.setInt(3, idEmpresa); // <-- SE INYECTA LA EMPRESA

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

    // ==========================================
    // VINCULAR ALUMNO Y SUMAR RUTINAS (MÁXIMO 10)
    // ==========================================
    public String vincularAlumno(int idUsuarioEntrenador, com.mathew.gimnasio.modelos.AsignarAlumnoDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt(1);
            else return "Error: Entrenador no encontrado.";

            // 1. REGLA DE NEGOCIO: Validar que no tenga más de 10 rutinas activas
            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_cliente = ? AND id_entrenador = ? AND activa = TRUE");
            ps.setInt(1, datos.getIdCliente());
            ps.setInt(2, idEntrenador);
            rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) >= 10) {
                return "LÍMITE ALCANZADO: El alumno ya tiene 10 rutinas activas. Elimina una antigua para asignar nuevas.";
            }

            // 2. Si eligió una rutina, la CLONAMOS y se SUMA a su colección personal
            if (datos.getIdRutinaAsignada() > 0) {
                String nombreClon = "Rutina Asignada";
                ps = conn.prepareStatement("SELECT nombre_rutina FROM rutinas WHERE id_rutina = ?");
                ps.setInt(1, datos.getIdRutinaAsignada());
                rs = ps.executeQuery();
                if(rs.next()) nombreClon = rs.getString(1);

                ps = conn.prepareStatement("INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina");
                ps.setInt(1, datos.getIdCliente());
                ps.setInt(2, idEntrenador);
                ps.setString(3, nombreClon);
                rs = ps.executeQuery();

                int nuevaRutinaId = 0;
                if(rs.next()) nuevaRutinaId = rs.getInt(1);

                if(nuevaRutinaId > 0) {
                    ps = conn.prepareStatement("INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) SELECT ?, id_ejercicio, series, repeticiones FROM detalle_rutinas WHERE id_rutina = ?");
                    ps.setInt(1, nuevaRutinaId);
                    ps.setInt(2, datos.getIdRutinaAsignada());
                    ps.executeUpdate();
                }
            } else {
                // Si no mandó rutina y no tiene ninguna, creamos una de inicio
                ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_cliente = ? AND id_entrenador = ?");
                ps.setInt(1, datos.getIdCliente());
                ps.setInt(2, idEntrenador);
                rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    ps = conn.prepareStatement("INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, 'Plan Inicial', CURRENT_DATE, TRUE)");
                    ps.setInt(1, datos.getIdCliente());
                    ps.setInt(2, idEntrenador);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return "OK";
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch(Exception ex) {}
            e.printStackTrace();
            return "Error de Base de Datos.";
        } finally {
            try { if (conn != null) conn.close(); } catch(Exception ex) {}
        }
    }

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
}