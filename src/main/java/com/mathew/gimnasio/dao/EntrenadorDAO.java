package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EntrenadorDAO {

    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario) {
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. DATOS DEL ENTRENADOR (Nombre y Especialidad)
            String sqlEnt = "SELECT id_entrenador, nombre || ' ' || apellido as n, especialidad FROM entrenadores WHERE id_usuario = ?";
            PreparedStatement ps = conn.prepareStatement(sqlEnt);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            int idEntrenador = 0;
            if (rs.next()) {
                idEntrenador = rs.getInt("id_entrenador");
                dto.nombre = rs.getString("n");
                dto.especialidad = rs.getString("especialidad");
            } else {
                return null; // Si no es entrenador, devolvemos null
            }

            // 2. LISTA DE RUTINAS CREADAS (Para el panel derecho)
            ps = conn.prepareStatement("SELECT nombre_rutina FROM rutinas WHERE id_entrenador = ?");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            while(rs.next()) {
                dto.listaRutinas.add(rs.getString("nombre_rutina"));
            }
            dto.rutinasCreadas = dto.listaRutinas.size();

            // 3. SUS ALUMNOS (Corrección: Traemos el ID real)
            String sqlAlumnos = "SELECT DISTINCT c.id_cliente, c.nombre || ' ' || c.apellido as n, " + // <--- Agregamos c.id_cliente
                    "COALESCE(m.nombre, 'Sin Plan') as plan, " +
                    "r.nombre_rutina, " +
                    "CASE WHEN h.id_historial IS NOT NULL THEN 'SI' ELSE 'NO' END as termino " +
                    "FROM rutinas r " +
                    "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "LEFT JOIN historial_entrenamientos h ON c.id_cliente = h.id_cliente AND h.fecha = CURRENT_DATE " +
                    "WHERE r.id_entrenador = ?";

            ps = conn.prepareStatement(sqlAlumnos);
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            while(rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));

                // Aquí usamos el nuevo constructor con el ID real
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_cliente"), // <--- ID REAL DE LA BDD
                        rs.getString("n"),
                        rs.getString("plan"),
                        rs.getString("nombre_rutina"),
                        yaTermino
                ));
            }
            dto.totalAlumnos = dto.listaAlumnos.size();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dto;
    }
    // NUEVO Metodo crear rutina
    public boolean crearRutina(int idUsuarioEntrenador, com.mathew.gimnasio.modelos.NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Obtener ID del Entrenador
            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt("id_entrenador");
            else return false;

            // ---------------------------------------------------------
            // 1.5. ¡EL FIX MÁGICO! LIMPIAR EL HISTORIAL DE HOY
            // Si le asigno rutina nueva, borramos el "Check Verde" de hoy para que tenga que hacerlo de nuevo.
            String sqlReset = "DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            PreparedStatement psReset = conn.prepareStatement(sqlReset);
            psReset.setInt(1, datos.idCliente);
            psReset.executeUpdate();
            // ---------------------------------------------------------

            // 2. Insertar la Rutina (IGUAL QUE ANTES)
            String sqlRutina = "INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion) VALUES (?, ?, ?, CURRENT_DATE) RETURNING id_rutina";
            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, datos.idCliente);
            ps.setInt(2, idEntrenador);
            ps.setString(3, datos.nombreRutina);
            rs = ps.executeQuery();

            int idRutina = 0;
            if (rs.next()) idRutina = rs.getInt(1);

            // 3. Insertar los Ejercicios (IGUAL QUE ANTES)
            String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
            ps = conn.prepareStatement(sqlDetalle);

            for (Integer idEjercicio : datos.idsEjercicios) {
                ps.setInt(1, idRutina);
                ps.setInt(2, idEjercicio);
                ps.addBatch();
            }
            ps.executeBatch();

            conn.commit(); // Confirmar cambios
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }
}