package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * DAO DASHBOARD DEL CLIENTE
 * Esta clase es la encargada de entrar a la base de datos y recolectar toda la información
 * que el cliente ve al entrar a su perfil. Se asegura de traer datos frescos y reales.
 */
public class ClienteDashboardDAO {

    /**
     * OBTENER TODA LA INFO DEL DASHBOARD
     * Este mEtodo hace varias consultas a la vez para armar un resumen completo del cliente.
     * @param idUsuario El ID con el que el cliente inició sesión.
     */
    public ResumenClienteDTO obtenerInfoDashboard(int idUsuario) {
        ResumenClienteDTO dto = new ResumenClienteDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            /* 1.PERFIL Y ESTADO DE MEMBRESÍA
             * Aquí traemos el nombre, email y teléfono. También revisamos si su plan (Smart o Black)
             * sigue vigente comparando la fecha de vencimiento con el día de hoy.
             */
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
            } else return null; // Si no hay cliente, no seguimos buscando

            /* 2. HISTORIAL DE ASISTENCIAS
             * Buscamos las últimas 5 veces que el cliente escaneó su QR para entrar.
             * Formateamos la fecha y hora directamente desde la base de datos para que sea fácil de leer.
             */
            dto.historialAsistencias = new ArrayList<>();
            ps = conn.prepareStatement("SELECT to_char(fecha_hora_ingreso, 'YYYY-MM-DD') as f, to_char(fecha_hora_ingreso, 'HH24:MI') as h FROM asistencias WHERE id_cliente = ? ORDER BY fecha_hora_ingreso DESC LIMIT 5");
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();
            while(rs.next()) {
                dto.historialAsistencias.add(new ResumenClienteDTO.AsistenciaSimple(rs.getString("f"), rs.getString("h")));
            }

            /* 3. RUTINA DEL DÍA
             * Este código tiene una regla especial: Solo muestra la rutina si el entrenador la creó HOY.
             * Si hay rutina, también traemos la lista detallada de ejercicios (series y reps).
             */
            dto.ejercicios = new ArrayList<>();
            String sqlRutina = "SELECT r.id_rutina, r.nombre_rutina, COALESCE(e.nombre, 'Staff') as ent " +
                    "FROM rutinas r " +
                    "LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "WHERE r.id_cliente = ? " +
                    "AND r.fecha_creacion = CURRENT_DATE " + // <--- ¡AQUÍ ESTÁ LA MAGIA DE LA FECHA!
                    "ORDER BY r.id_rutina DESC LIMIT 1";

            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            if(rs.next()){
                dto.nombreRutina = rs.getString("nombre_rutina");
                dto.entrenador = rs.getString("ent");
                int idR = rs.getInt("id_rutina");

                // Buscamos los ejercicios específicos de esta rutina (Ej: Press Banca 4x12)
                ps = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                ps.setInt(1, idR);
                ResultSet rsEj = ps.executeQuery();
                while(rsEj.next()) {
                    dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rsEj.getString("nombre_ejercicio"), rsEj.getString("sr")));
                }
            }

            /* 4. ¿YA TERMINÓ DE ENTRENAR?
             * Revisamos si el cliente ya presionó el botón de "Terminar" hoy.
             * Esto sirve para que el Dashboard sepa si debe bloquear el botón o mostrar un mensaje de éxito.
             */
            String sqlCheck = "SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            ps = conn.prepareStatement(sqlCheck);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            dto.rutinaTerminadaHoy = rs.next(); // True si ya existe registro hoy

        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }

    /**
     * REGISTRAR TÉRMINO DE RUTINA
     * Guarda en la base de datos el momento exacto en que el cliente termina su entrenamiento.
     * @param idUsuario ID del usuario que presiona el botón.
     * @return boolean True si se guardó, False si algo falló o si ya había entrenado hoy.
     */
    public boolean registrarTerminoRutina(int idUsuario) {
        try (Connection conn = ConexionDB.getConnection()) {
            // 1. Buscamos qué rutina está haciendo el cliente actualmente
            String sqlInfo = "SELECT id_cliente, id_rutina FROM rutinas WHERE id_cliente = (SELECT id_cliente FROM clientes WHERE id_usuario = ?) ORDER BY id_rutina DESC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sqlInfo);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                /* 2. INSERTAMOS EN EL HISTORIAL
                 * Usamos una validación especial para que no se guarden dos registros el mismo día
                 * si el cliente presiona el botón muchas veces por error.
                 */
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