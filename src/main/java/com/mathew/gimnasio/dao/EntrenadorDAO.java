package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class EntrenadorDAO {

    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario) {
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. OBTENER ID ENTRENADOR
            String sqlEnt = "SELECT id_entrenador, nombre || ' ' || apellido as n, especialidad FROM entrenadores WHERE id_usuario = ?";
            PreparedStatement ps = conn.prepareStatement(sqlEnt);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            int idEntrenador = 0;
            if(rs.next()) {
                idEntrenador = rs.getInt("id_entrenador");
                dto.nombre = rs.getString("n");
                dto.especialidad = rs.getString("especialidad");
            }

            // 2. CONTEOS
            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            if(rs.next()) dto.rutinasCreadas = rs.getInt(1);

            ps = conn.prepareStatement("SELECT COUNT(DISTINCT id_cliente) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            if(rs.next()) dto.totalAlumnos = rs.getInt(1);

            // 3. LISTA DE ALUMNOS
            dto.listaAlumnos = new ArrayList<>();
            String sqlAlumnos = "SELECT DISTINCT c.id_cliente, c.nombre || ' ' || c.apellido as n, " +
                    "COALESCE(m.nombre, 'Sin Plan') as plan, " +
                    "r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as termino " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE r.id_entrenador = ? AND r.activa = TRUE"; // Solo activos

            ps = conn.prepareStatement(sqlAlumnos);
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            while(rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_cliente"),
                        rs.getString("n"),
                        rs.getString("plan"),
                        rs.getString("nombre_rutina"),
                        yaTermino
                ));
            }

            // 4. LISTA DE RUTINAS (BIBLIOTECA) - AHORA CON ID
            dto.listaRutinas = new ArrayList<>();
            // Quitamos el "AND activa = TRUE" para traer todo
            ps = conn.prepareStatement("SELECT id_rutina, nombre_rutina, activa, id_cliente FROM rutinas WHERE id_entrenador = ? ORDER BY id_rutina DESC");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            while(rs.next()) {
                EntrenadorDashboardDTO.RutinaItem item = new EntrenadorDashboardDTO.RutinaItem(
                        rs.getInt("id_rutina"),
                        rs.getString("nombre_rutina"),
                        rs.getBoolean("activa"),
                        rs.getInt("id_cliente")
                );

                // Cargar IDs de ejercicios para poder editar luego
                PreparedStatement ps2 = conn.prepareStatement("SELECT id_ejercicio FROM detalle_rutinas WHERE id_rutina = ?");
                ps2.setInt(1, item.id);
                ResultSet rs2 = ps2.executeQuery();
                while(rs2.next()) item.idsEjercicios.add(rs2.getInt(1));

                dto.listaRutinas.add(item);
            }

        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }

    // CREAR RUTINA (Con limpieza de historial diario)
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

            // RESET DIARIO: Si asigno rutina, borro el "Completado" de hoy
            String sqlReset = "DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            PreparedStatement psReset = conn.prepareStatement(sqlReset);
            psReset.setInt(1, datos.idCliente);
            psReset.executeUpdate();

            // INSERTAR RUTINA
            String sqlRutina = "INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina";
            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, datos.idCliente);
            ps.setInt(2, idEntrenador);
            ps.setString(3, datos.nombreRutina);
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

    // --- NUEVO MÉTODO: DESACTIVAR (BORRADO LÓGICO) ---
    public boolean desactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            // No borramos (DELETE), solo actualizamos el estado
            String sql = "UPDATE rutinas SET activa = FALSE WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- MÉTODO AGENDA (Del paso anterior) ---
    public java.util.List<EntrenadorDashboardDTO.AlumnoResumen> obtenerAgendaHoy(int idUsuarioEntrenador) {
        java.util.List<EntrenadorDashboardDTO.AlumnoResumen> agenda = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT c.id_cliente, c.nombre || ' ' || c.apellido as n, r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as completo " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE e.id_usuario = ? AND r.activa = TRUE"; // Solo activas

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                boolean termino = "SI".equals(rs.getString("completo"));
                agenda.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_cliente"), rs.getString("n"), "Hoy", rs.getString("nombre_rutina"), termino
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return agenda;
    }
    // --- MÉTODO ACTUALIZAR (EDITAR) ---
    public boolean actualizarRutina(int idRutina, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            // 1. Actualizar nombre y cliente
            String sqlUpdate = "UPDATE rutinas SET nombre_rutina = ?, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, datos.nombreRutina);
            ps.setInt(2, datos.idCliente);
            ps.setInt(3, idRutina);
            ps.executeUpdate();

            // 2. Borrar ejercicios viejos
            ps = conn.prepareStatement("DELETE FROM detalle_rutinas WHERE id_rutina = ?");
            ps.setInt(1, idRutina);
            ps.executeUpdate();

            // 3. Insertar ejercicios nuevos
            String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
            ps = conn.prepareStatement(sqlDetalle);
            for (Integer idEjercicio : datos.idsEjercicios) {
                ps.setInt(1, idRutina);
                ps.setInt(2, idEjercicio);
                ps.addBatch();
            }
            ps.executeBatch();

            // 4. Resetear historial de hoy (por si cambiaron la rutina)
            ps = conn.prepareStatement("DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE");
            ps.setInt(1, datos.idCliente);
            ps.executeUpdate();

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if(conn!=null) conn.rollback(); } catch(Exception ex){}
            return false;
        } finally { try { if(conn!=null) conn.close(); } catch(Exception ex){} }
    }

    // --- MÉTODO REACTIVAR (SACAR DE LA BASURA) ---
    public boolean reactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "UPDATE rutinas SET activa = TRUE WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}