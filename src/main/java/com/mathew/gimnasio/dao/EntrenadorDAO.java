package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import com.mathew.gimnasio.modelos.AsignarAlumnoDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class EntrenadorDAO {

    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario) {
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();
        try (Connection conn = ConexionDB.getConnection()) {
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

            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente IS NULL");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            if (rs.next()) dto.rutinasCreadas = rs.getInt(1);

            ps = conn.prepareStatement("SELECT COUNT(DISTINCT id_cliente) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE AND id_cliente IS NOT NULL");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            if (rs.next()) dto.totalAlumnos = rs.getInt(1);

            dto.listaAlumnos = new ArrayList<>();
            // CORRECCIÓN CLAVE: Enviamos el c.id_usuario al frontend, no el c.id_cliente
            String sqlAlumnos = "SELECT DISTINCT c.id_usuario, c.nombre || ' ' || c.apellido as n, " +
                    "COALESCE(m.nombre, 'Sin Plan') as plan, r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as termino " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE r.id_entrenador = ? AND r.activa = TRUE";

            ps = conn.prepareStatement(sqlAlumnos);
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            while (rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_usuario"), rs.getString("n"), rs.getString("plan"), rs.getString("nombre_rutina"), yaTermino
                ));
            }

            dto.listaRutinas = new ArrayList<>();
            ps = conn.prepareStatement("SELECT id_rutina, nombre_rutina, activa, id_cliente FROM rutinas WHERE id_entrenador = ? AND id_cliente IS NULL ORDER BY id_rutina DESC");
            ps.setInt(1, idEntrenador);
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

    public boolean crearRutina(int idUsuarioEntrenador, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt("id_entrenador");
            else return false;

            // CORRECCIÓN: id_cliente = NULL para que sea una Plantilla Maestra en la biblioteca
            String sqlRutina = "INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (NULL, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina";
            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, idEntrenador);
            ps.setString(2, datos.nombreRutina);
            rs = ps.executeQuery();

            int idRutina = 0;
            if (rs.next()) idRutina = rs.getInt(1);

            String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
            ps = conn.prepareStatement(sqlDetalle);

            for (Integer idEjercicio : datos.idsEjercicios) {
                ps.setInt(1, idRutina);
                ps.setInt(2, idEjercicio);
                ps.addBatch();
            }
            ps.executeBatch();

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

    public boolean actualizarRutina(int idRutina, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            // CORRECCIÓN: Al editar en biblioteca, garantizamos que siga siendo Plantilla (NULL)
            String sqlUpdate = "UPDATE rutinas SET nombre_rutina = ?, id_cliente = NULL WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, datos.nombreRutina);
            ps.setInt(2, idRutina);
            ps.executeUpdate();

            ps = conn.prepareStatement("DELETE FROM detalle_rutinas WHERE id_rutina = ?");
            ps.setInt(1, idRutina);
            ps.executeUpdate();

            String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
            ps = conn.prepareStatement(sqlDetalle);
            for (Integer idEjercicio : datos.idsEjercicios) {
                ps.setInt(1, idRutina);
                ps.setInt(2, idEjercicio);
                ps.addBatch();
            }
            ps.executeBatch();

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

    // CORRECCIÓN "FANTASMA": Restaura la rutina y se asegura de que es una plantilla (NULL)
    public boolean reactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "UPDATE rutinas SET activa = TRUE, id_cliente = NULL WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public java.util.List<EntrenadorDashboardDTO.AlumnoResumen> obtenerAgendaHoy(int idUsuarioEntrenador) {
        java.util.List<EntrenadorDashboardDTO.AlumnoResumen> agenda = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT c.id_usuario, c.nombre || ' ' || c.apellido as n, r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as completo " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE e.id_usuario = ? AND r.activa = TRUE";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuarioEntrenador);
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
    // MÉTODO CORREGIDO: VINCULAR NUEVO ALUMNO
    // ==========================================
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

            // TRADUCTOR MAGICO: Convertimos el id_usuario del Frontend al id_cliente real de la BD
            int realIdCliente = 0;
            ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?");
            ps.setInt(1, datos.getIdCliente());
            rs = ps.executeQuery();
            if (rs.next()) realIdCliente = rs.getInt(1);
            else return false;

            // Desactivamos cualquier rutina anterior de este alumno
            ps = conn.prepareStatement("UPDATE rutinas SET activa = FALSE WHERE id_cliente = ? AND id_entrenador = ?");
            ps.setInt(1, realIdCliente);
            ps.setInt(2, idEntrenador);
            ps.executeUpdate();

            // Si eligió una rutina, creamos una CLONACIÓN para este alumno
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

            // Limpiamos su historial de hoy
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

    // ==========================================
    // MÉTODO CORREGIDO: DESVINCULAR ALUMNO
    // ==========================================
    public boolean desvincularAlumno(int idUsuarioEntrenador, int idUsuarioCliente) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt(1);

            // Traducimos el ID de nuevo
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

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE");
            ps2.setInt(1, realIdCliente);
            ps2.executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}