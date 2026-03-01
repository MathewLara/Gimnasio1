package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.EjercicioEnRutinaDTO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import com.mathew.gimnasio.modelos.EntrenadorDTO;
import com.mathew.gimnasio.util.SecurityUtil;

import java.sql.*;
import java.util.ArrayList;

/**
 * CLASE DAO DEL ENTRENADOR
 * Esta clase es el motor que permite a los entrenadores gestionar el gimnasio.
 * Contiene la lógica para consultar estadísticas, ver el progreso de los alumnos
 * y realizar acciones como crear, editar o "borrar" rutinas.
 */
public class EntrenadorDAO {

    /**
     * OBTENER TABLERO PRINCIPAL
     * Reúne toda la información que el entrenador ve al entrar a su panel.
     *
     * @param idUsuario ID de la cuenta del profesor logueado.
     */
    public EntrenadorDashboardDTO obtenerDashboard(int idUsuario) {
        EntrenadorDashboardDTO dto = new EntrenadorDashboardDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. IDENTIFICAR AL ENTRENADOR
            // Buscamos el nombre y la especialidad del profesor usando su ID de usuario.
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

            // PASO 2. CONTADORES DE ESTADÍSTICAS
            // Contamos cuántas rutinas tiene activas actualmente.
            ps = conn.prepareStatement("SELECT COUNT(*) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            if (rs.next()) dto.rutinasCreadas = rs.getInt(1);

            // Contamos cuántos alumnos diferentes tiene asignados.
            ps = conn.prepareStatement("SELECT COUNT(DISTINCT id_cliente) FROM rutinas WHERE id_entrenador = ? AND activa = TRUE");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            if (rs.next()) dto.totalAlumnos = rs.getInt(1);

            // 3. LISTA DE ALUMNOS Y SU PROGRESO DIARIO
            // Traemos a los alumnos y revisamos si ya presionaron el botón "Terminar" hoy.
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
            while (rs.next()) {
                boolean yaTermino = "SI".equals(rs.getString("termino"));
                dto.listaAlumnos.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_cliente"),
                        rs.getString("n"),
                        rs.getString("plan"),
                        rs.getString("nombre_rutina"),
                        yaTermino
                ));
            }

            // 4. BIBLIOTECA DE RUTINAS
            // Listamos todas las rutinas (incluso las inactivas) para que el profesor pueda administrarlas.
            dto.listaRutinas = new ArrayList<>();
            ps = conn.prepareStatement("SELECT id_rutina, nombre_rutina, activa, id_cliente FROM rutinas WHERE id_entrenador = ? ORDER BY id_rutina DESC");
            ps.setInt(1, idEntrenador);
            rs = ps.executeQuery();
            while (rs.next()) {
                EntrenadorDashboardDTO.RutinaItem item = new EntrenadorDashboardDTO.RutinaItem(
                        rs.getInt("id_rutina"),
                        rs.getString("nombre_rutina"),
                        rs.getBoolean("activa"),
                        rs.getInt("id_cliente")
                );

                // También cargamos qué ejercicios tiene cada rutina para poder editarlos después.
                PreparedStatement ps2 = conn.prepareStatement("SELECT id_ejercicio FROM detalle_rutinas WHERE id_rutina = ?");
                ps2.setInt(1, item.id);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) item.idsEjercicios.add(rs2.getInt(1));

                dto.listaRutinas.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dto;
    }

    /**
     * CREAR NUEVA RUTINA
     * Registra una rutina y sus ejercicios. Usa "transacciones" para que si algo falla,
     * no se guarde nada a medias.
     */
    public boolean crearRutina(int idUsuarioEntrenador, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciamos modo "todo o nada"

            // 1. Buscamos el ID interno del entrenador.
            int idEntrenador = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_entrenador FROM entrenadores WHERE id_usuario = ?");
            ps.setInt(1, idUsuarioEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) idEntrenador = rs.getInt("id_entrenador");
            else return false;

            // 2. Limpieza: Si le asignamos una rutina nueva, borramos el "completado" de hoy.
            String sqlReset = "DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            PreparedStatement psReset = conn.prepareStatement(sqlReset);
            psReset.setInt(1, datos.idCliente);
            psReset.executeUpdate();

            // 3. Guardamos la cabecera de la rutina.
            String sqlRutina = "INSERT INTO rutinas (id_cliente, id_entrenador, nombre_rutina, fecha_creacion, activa) VALUES (?, ?, ?, CURRENT_DATE, TRUE) RETURNING id_rutina";
            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, datos.idCliente);
            ps.setInt(2, idEntrenador);
            ps.setString(3, datos.nombreRutina);
            rs = ps.executeQuery();

            int idRutina = 0;
            if (rs.next()) idRutina = rs.getInt(1);

            // 4. Guardamos ejercicios (con series/reps/descanso si vienen en ejercicios)
            if (datos.ejercicios != null && !datos.ejercicios.isEmpty()) {
                try {
                    String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones, descanso) VALUES (?, ?, ?, ?, ?)";
                    ps = conn.prepareStatement(sqlDetalle);
                    for (EjercicioEnRutinaDTO e : datos.ejercicios) {
                        ps.setInt(1, idRutina);
                        ps.setInt(2, e.idEjercicio);
                        ps.setString(3, e.series != null ? e.series : "4");
                        ps.setString(4, e.repeticiones != null ? e.repeticiones : "12");
                        ps.setString(5, e.descanso != null ? e.descanso : "");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                } catch (SQLException ex) {
                    ps = conn.prepareStatement("INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, ?, ?)");
                    for (EjercicioEnRutinaDTO e : datos.ejercicios) {
                        ps.setInt(1, idRutina);
                        ps.setInt(2, e.idEjercicio);
                        ps.setString(3, e.series != null ? e.series : "4");
                        ps.setString(4, e.repeticiones != null ? e.repeticiones : "12");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } else if (datos.idsEjercicios != null) {
                String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones, descanso) VALUES (?, ?, '4', '12', ?)";
                try {
                    ps = conn.prepareStatement(sqlDetalle);
                    for (Integer idEjercicio : datos.idsEjercicios) {
                        ps.setInt(1, idRutina);
                        ps.setInt(2, idEjercicio);
                        ps.setString(3, "60 seg");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                } catch (SQLException ex) {
                    sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
                    ps = conn.prepareStatement(sqlDetalle);
                    for (Integer idEjercicio : datos.idsEjercicios) {
                        ps.setInt(1, idRutina);
                        ps.setInt(2, idEjercicio);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit(); // Confirmamos que todo se guardó bien
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ex) {
            } // Si falló algo, cancelamos todo
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * DESACTIVAR RUTINA (BORRADO LÓGICO)
     * No borramos la rutina de la base de datos para no perder el historial;
     * simplemente la marcamos como "inactiva".
     */
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

    // --- MeTODO AGENDA (Del paso anterior) ---
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
            while (rs.next()) {
                boolean termino = "SI".equals(rs.getString("completo"));
                agenda.add(new EntrenadorDashboardDTO.AlumnoResumen(
                        rs.getInt("id_cliente"), rs.getString("n"), "Hoy", rs.getString("nombre_rutina"), termino
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return agenda;
    }

    /**
     * ACTUALIZAR O EDITAR RUTINA
     * Modifica el nombre, el cliente y reemplaza todos los ejercicios antiguos por los nuevos.
     */
    public boolean actualizarRutina(int idRutina, NuevaRutinaDTO datos) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            // 1. Cambiamos los datos principales.
            String sqlUpdate = "UPDATE rutinas SET nombre_rutina = ?, id_cliente = ? WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sqlUpdate);
            ps.setString(1, datos.nombreRutina);
            ps.setInt(2, datos.idCliente);
            ps.setInt(3, idRutina);
            ps.executeUpdate();

            // 2. Quitamos los ejercicios anteriores.
            ps = conn.prepareStatement("DELETE FROM detalle_rutinas WHERE id_rutina = ?");
            ps.setInt(1, idRutina);
            ps.executeUpdate();

            // 3. Insertamos ejercicios
            if (datos.ejercicios != null && !datos.ejercicios.isEmpty()) {
                String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones, descanso) VALUES (?, ?, ?, ?, ?)";
                try {
                    ps = conn.prepareStatement(sqlDetalle);
                    for (EjercicioEnRutinaDTO e : datos.ejercicios) {
                        ps.setInt(1, idRutina);
                        ps.setInt(2, e.idEjercicio);
                        ps.setString(3, e.series != null ? e.series : "4");
                        ps.setString(4, e.repeticiones != null ? e.repeticiones : "12");
                        ps.setString(5, e.descanso != null ? e.descanso : "");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                } catch (SQLException sqle) {
                    sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, ?, ?)";
                    ps = conn.prepareStatement(sqlDetalle);
                    for (EjercicioEnRutinaDTO e : datos.ejercicios) {
                        ps.setInt(1, idRutina);
                        ps.setInt(2, e.idEjercicio);
                        ps.setString(3, e.series != null ? e.series : "4");
                        ps.setString(4, e.repeticiones != null ? e.repeticiones : "12");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } else if (datos.idsEjercicios != null) {
                String sqlDetalle = "INSERT INTO detalle_rutinas (id_rutina, id_ejercicio, series, repeticiones) VALUES (?, ?, '4 Series', '12 Reps')";
                ps = conn.prepareStatement(sqlDetalle);
                for (Integer idEjercicio : datos.idsEjercicios) {
                    ps.setInt(1, idRutina);
                    ps.setInt(2, idEjercicio);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 4. Si editamos la rutina, reseteamos el historial de hoy para que el alumno pueda marcarla otra vez.
            ps = conn.prepareStatement("DELETE FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE");
            ps.setInt(1, datos.idCliente);
            ps.executeUpdate();

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ex) {
            }
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * REACTIVAR RUTINA
     * Recupera una rutina que había sido desactivada anteriormente.
     */
    public boolean reactivarRutina(int idRutina) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "UPDATE rutinas SET activa = TRUE WHERE id_rutina = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idRutina);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========== CRUD ENTRENADORES (RF04) ==========

    public String listarEntrenadoresJSON() {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT e.id_entrenador, e.id_usuario, e.nombre, e.apellido, e.email, e.especialidad, u.usuario, u.activo " +
                    "FROM entrenadores e JOIN usuarios u ON e.id_usuario = u.id_usuario ORDER BY e.id_entrenador";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"idEntrenador\":").append(rs.getInt("id_entrenador"))
                        .append(",\"idUsuario\":").append(rs.getInt("id_usuario"))
                        .append(",\"nombre\":\"").append(escape(rs.getString("nombre")))
                        .append("\",\"apellido\":\"").append(escape(rs.getString("apellido")))
                        .append("\",\"email\":\"").append(escape(rs.getString("email")))
                        .append("\",\"usuario\":\"").append(escape(rs.getString("usuario")))
                        .append("\",\"especialidad\":\"").append(escape(rs.getString("especialidad")))
                        .append("\",\"activo\":").append(rs.getBoolean("activo")).append("}");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    public String obtenerEntrenadorJSON(int idEntrenador) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT e.id_entrenador, e.id_usuario, e.nombre, e.apellido, e.email, e.especialidad, e.notas_desempeno, u.usuario, u.activo " +
                    "FROM entrenadores e JOIN usuarios u ON e.id_usuario = u.id_usuario WHERE e.id_entrenador = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idEntrenador);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "{\"idEntrenador\":" + rs.getInt("id_entrenador") +
                        ",\"idUsuario\":" + rs.getInt("id_usuario") +
                        ",\"nombre\":\"" + escape(rs.getString("nombre")) + "\"" +
                        ",\"apellido\":\"" + escape(rs.getString("apellido")) + "\"" +
                        ",\"email\":\"" + escape(rs.getString("email")) + "\"" +
                        ",\"usuario\":\"" + escape(rs.getString("usuario")) + "\"" +
                        ",\"especialidad\":\"" + escape(rs.getString("especialidad")) + "\"" +
                        ",\"notasDesempeno\":\"" + escape(rs.getString("notas_desempeno")) + "\"" +
                        ",\"activo\":" + rs.getBoolean("activo") + "}";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean crearEntrenador(EntrenadorDTO dto) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);
            String sqlUsu = "INSERT INTO usuarios (id_rol, usuario, contrasena, activo, nombre, apellido) VALUES (3, ?, ?, true, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sqlUsu, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getUsuario());
            ps.setString(2, SecurityUtil.encriptar(dto.getContrasena() != null ? dto.getContrasena() : "123456"));
            ps.setString(3, dto.getNombre());
            ps.setString(4, dto.getApellido());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) { conn.rollback(); return false; }
            int idUsuario = rs.getInt(1);
            String sqlEnt = "INSERT INTO entrenadores (id_usuario, nombre, apellido, email, especialidad) VALUES (?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sqlEnt);
            ps.setInt(1, idUsuario);
            ps.setString(2, dto.getNombre());
            ps.setString(3, dto.getApellido());
            ps.setString(4, dto.getEmail());
            ps.setString(5, dto.getEspecialidad());
            ps.executeUpdate();
            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException ex) {}
        }
    }

    public boolean actualizarEntrenador(int idEntrenador, EntrenadorDTO dto) {
        try (Connection conn = ConexionDB.getConnection()) {
            int idUsuario = 0;
            PreparedStatement ps = conn.prepareStatement("SELECT id_usuario FROM entrenadores WHERE id_entrenador = ?");
            ps.setInt(1, idEntrenador);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;
            idUsuario = rs.getInt("id_usuario");

            String sqlEnt = "UPDATE entrenadores SET nombre=?, apellido=?, email=?, especialidad=? WHERE id_entrenador=?";
            ps = conn.prepareStatement(sqlEnt);
            ps.setString(1, dto.getNombre());
            ps.setString(2, dto.getApellido());
            ps.setString(3, dto.getEmail());
            ps.setString(4, dto.getEspecialidad());
            ps.setInt(5, idEntrenador);
            ps.executeUpdate();

            if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
                ps = conn.prepareStatement("UPDATE usuarios SET usuario=?, contrasena=?, nombre=?, apellido=? WHERE id_usuario=?");
                ps.setString(1, dto.getUsuario());
                ps.setString(2, SecurityUtil.encriptar(dto.getContrasena()));
                ps.setString(3, dto.getNombre());
                ps.setString(4, dto.getApellido());
                ps.setInt(5, idUsuario);
            } else {
                ps = conn.prepareStatement("UPDATE usuarios SET usuario=?, nombre=?, apellido=? WHERE id_usuario=?");
                ps.setString(1, dto.getUsuario());
                ps.setString(2, dto.getNombre());
                ps.setString(3, dto.getApellido());
                ps.setInt(4, idUsuario);
            }
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eliminarEntrenador(int idEntrenador) {
        try (Connection conn = ConexionDB.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT id_usuario FROM entrenadores WHERE id_entrenador = ?");
            ps.setInt(1, idEntrenador);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;
            int idUsuario = rs.getInt("id_usuario");
            ps = conn.prepareStatement("UPDATE usuarios SET activo = FALSE WHERE id_usuario = ?");
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escape(String s) {
        return s != null ? s.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }
}