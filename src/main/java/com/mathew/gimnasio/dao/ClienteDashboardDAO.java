package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ClienteDashboardDAO {

    // REEMPLAZAR EL Metodo  obtenerInfoDashboard CON ESTO:
    public ResumenClienteDTO obtenerInfoDashboard(int idUsuario) {
        ResumenClienteDTO dto = new ResumenClienteDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. PERFIL + MEMBRESÍA (Consulta arreglada)
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

                // DATOS NUEVOS
                dto.nombrePlan = rs.getString("plan") != null ? rs.getString("plan") : "Sin Membresía";
                dto.precioPlan = rs.getDouble("precio");
                dto.fechaVencimiento = rs.getString("fecha_vencimiento");
                dto.estadoMembresia = rs.getString("estado");
            } else return null;

            // ... (AQUÍ SIGUE TU CÓDIGO DE ASISTENCIAS Y RUTINAS, NO LO BORRES) ...
            // Copia y pega aquí la parte 2 (Asistencias) y 3 (Rutinas) que ya tenías antes.


            // 2. ASISTENCIAS
            dto.historialAsistencias = new ArrayList<>();
            ps = conn.prepareStatement("SELECT to_char(fecha_hora_ingreso, 'YYYY-MM-DD') as f, to_char(fecha_hora_ingreso, 'HH24:MI') as h FROM asistencias WHERE id_cliente = ? ORDER BY fecha_hora_ingreso DESC LIMIT 5");
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();
            while(rs.next()) dto.historialAsistencias.add(new ResumenClienteDTO.AsistenciaSimple(rs.getString("f"), rs.getString("h")));

            // 3. RUTINA
            dto.ejercicios = new ArrayList<>();
            ps = conn.prepareStatement("SELECT r.id_rutina, r.nombre_rutina, COALESCE(e.nombre, 'Staff') as ent FROM rutinas r LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador WHERE r.id_cliente = ? ORDER BY r.id_rutina DESC LIMIT 1");
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();
            if(rs.next()){
                dto.nombreRutina = rs.getString("nombre_rutina");
                dto.entrenador = rs.getString("ent");
                int idR = rs.getInt("id_rutina");
                ps = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                ps.setInt(1, idR);
                rs = ps.executeQuery();
                while(rs.next()) dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rs.getString("nombre_ejercicio"), rs.getString("sr")));
            }
            // 4. VERIFICAR SI YA TERMINÓ HOY (NUEVO)
            // ----------------------------------------------------
            // Usamos el idCliente que obtuvimos al principio del método
            // --- AGREGAR ESTO AL FINAL DEL MÉTODO obtenerInfoDashboard ---
            // 4. VERIFICAR SI YA TERMINÓ HOY
            String sqlCheck = "SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            ps = conn.prepareStatement(sqlCheck);
            ps.setInt(1, idCliente); // idCliente ya lo tienes definido arriba en tu código
            rs = ps.executeQuery();

            dto.rutinaTerminadaHoy = rs.next(); // Si hay resultado, es true.
            // -----------------------------------------------------------
            // ----------------------------------------------------

        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }
            // ----------------------------------------
    // Mismo archivo: com.mathew.gimnasio.dao.ClienteDashboardDAO

    // ... (tus otros métodos arriba) ...

    public boolean registrarTerminoRutina(int idUsuario) {
        try (Connection conn = ConexionDB.getConnection()) {
            // 1. Buscamos la rutina del cliente
            String sqlInfo = "SELECT id_cliente, id_rutina FROM rutinas WHERE id_cliente = (SELECT id_cliente FROM clientes WHERE id_usuario = ?) LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sqlInfo);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // 2. Insertamos en el historial (Solo si no existe registro de HOY)
                String sqlInsert = "INSERT INTO historial_entrenamientos (id_cliente, id_rutina, fecha) " +
                        "SELECT ?, ?, CURRENT_DATE WHERE NOT EXISTS " +
                        "(SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE)";

                PreparedStatement psIns = conn.prepareStatement(sqlInsert);
                psIns.setInt(1, rs.getInt("id_cliente"));
                psIns.setInt(2, rs.getInt("id_rutina"));
                psIns.setInt(3, rs.getInt("id_cliente")); // Para la validación del WHERE
                return psIns.executeUpdate() > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}