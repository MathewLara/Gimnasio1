package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ClienteDashboardDAO {

    public ResumenClienteDTO obtenerInfoDashboard(int idUsuario) {
        ResumenClienteDTO dto = new ResumenClienteDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            /* 1. PERFIL Y ESTADO DE MEMBRESÍA */
            String sql = "SELECT c.id_cliente, c.nombre || ' ' || c.apellido as n, c.email, c.telefono, " +
                    "m.nombre as plan, m.precio, c.fecha_vencimiento, " +
                    "CASE WHEN c.fecha_vencimiento >= CURRENT_DATE THEN 'Activo' ELSE 'Vencido' END as estado " +
                    "FROM clientes c " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "WHERE c.id_usuario = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            int idCliente = 0;
            if (rs.next()) {
                idCliente = rs.getInt("id_cliente");
                dto.nombreCompleto = rs.getString("n");
                dto.email = rs.getString("email");
                dto.telefono = rs.getString("telefono");
                dto.nombrePlan = rs.getString("plan") != null ? rs.getString("plan") : "Sin Membresía";
                dto.precioPlan = rs.getDouble("precio");
                dto.fechaVencimiento = rs.getString("fecha_vencimiento");
                dto.estadoMembresia = rs.getString("estado");
            } else return null;

            /* 2. HISTORIAL DE ASISTENCIAS (AJUSTE HORA ECUADOR) */
            dto.historialAsistencias = new ArrayList<>();
            // Restamos 5 horas a la entrada y a la salida para que coincida con Ecuador
            String sqlAsist = "SELECT to_char(fecha_hora_ingreso - INTERVAL '5 hours', 'YYYY-MM-DD') as f, " +
                    "to_char(fecha_hora_ingreso - INTERVAL '5 hours', 'HH24:MI:SS') as h_in, " +
                    "to_char(fecha_hora_salida - INTERVAL '5 hours', 'HH24:MI:SS') as h_out " +
                    "FROM asistencias WHERE id_cliente = ? ORDER BY fecha_hora_ingreso DESC LIMIT 5";

            ps = conn.prepareStatement(sqlAsist);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            boolean primeraFila = true;
            while(rs.next()) {
                String fecha = rs.getString("f");
                String horaIn = rs.getString("h_in");
                String horaOut = rs.getString("h_out") != null ? rs.getString("h_out") : "--:--";

                dto.historialAsistencias.add(new ResumenClienteDTO.AsistenciaSimple(fecha, horaIn, horaOut));

                if (primeraFila) {
                    dto.ultimoIngreso = horaIn;
                    dto.ultimaSalida = horaOut;
                    primeraFila = false;
                }
            }

            /* 3. RUTINA DEL DÍA (MANTENIENDO TU LÓGICA) */
            dto.ejercicios = new ArrayList<>();
            String sqlRutina = "SELECT r.id_rutina, r.nombre_rutina, COALESCE(e.nombre, 'Staff') as ent " +
                    "FROM rutinas r " +
                    "LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "WHERE r.id_cliente = ? " +
                    "AND r.fecha_creacion = CURRENT_DATE " +
                    "ORDER BY r.id_rutina DESC LIMIT 1";

            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            if(rs.next()){
                dto.nombreRutina = rs.getString("nombre_rutina");
                dto.entrenador = rs.getString("ent");
                int idR = rs.getInt("id_rutina");

                PreparedStatement psEj = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                psEj.setInt(1, idR);
                ResultSet rsEj = psEj.executeQuery();
                while(rsEj.next()) {
                    dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rsEj.getString("nombre_ejercicio"), rsEj.getString("sr")));
                }
            }

            /* 4. VERIFICACIÓN DE RUTINA TERMINADA */
            String sqlCheck = "SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            ps = conn.prepareStatement(sqlCheck);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();
            dto.rutinaTerminadaHoy = rs.next();

        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }

    /* MANTENIENDO TU FUNCIÓN DE REGISTRO DE TÉRMINO */
    public boolean registrarTerminoRutina(int idUsuario) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sqlInfo = "SELECT id_cliente, id_rutina FROM rutinas WHERE id_cliente = (SELECT id_cliente FROM clientes WHERE id_usuario = ?) ORDER BY id_rutina DESC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sqlInfo);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String sqlInsert = "INSERT INTO historial_entrenamientos (id_cliente, id_rutina, fecha) " +
                        "SELECT ?, ?, CURRENT_DATE WHERE NOT EXISTS " +
                        "(SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE)";

                PreparedStatement psIns = conn.prepareStatement(sqlInsert);
                psIns.setInt(1, rs.getInt("id_cliente"));
                psIns.setInt(2, rs.getInt("id_rutina"));
                psIns.setInt(3, rs.getInt("id_cliente"));
                return psIns.executeUpdate() > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
    // ==========================================
    // CANCELAR SUSCRIPCIÓN (NUEVO MÉTODO)
    // ==========================================
    public boolean cancelarSuscripcion(int idUsuario) {
        // Le restamos un día a la fecha de vencimiento para que pase a estado 'Vencido' inmediatamente
        String sql = "UPDATE clientes SET fecha_vencimiento = CURRENT_DATE - INTERVAL '1 day' WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}