package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List; // Agregamos esta importación

/**
 * DATA ACCESS OBJECT (DAO) DEL CLIENTE / SOCIO
 * Esta clase es el motor detrás del "Dashboard del Cliente" y del "Kiosko".
 * Su función principal es actuar como un Agregador (Aggregator Pattern),
 * extrayendo información de múltiples tablas (clientes, membresías, asistencias, rutinas)
 * para consolidarla en un solo viaje a la base de datos.
 */
public class ClienteDashboardDAO {

    /**
     * OBTENER TELEMETRÍA COMPLETA DEL SOCIO
     * @param idUsuario ID de autenticación del usuario logueado.
     * @param idEmpresa ID de la empresa del usuario.
     * @return ResumenClienteDTO con el perfil, estado financiero, historial y rutina del día.
     */
    public ResumenClienteDTO obtenerInfoDashboard(int idUsuario, int idEmpresa) {
        ResumenClienteDTO dto = new ResumenClienteDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            /* 1. PERFIL Y ESTADO DE MEMBRESÍA */
            String sql = "SELECT c.id_cliente, c.nombre || ' ' || c.apellido as n, c.email, c.telefono, " +
                    "m.nombre as plan, m.precio, c.fecha_vencimiento, c.cancelado, " +
                    "CASE WHEN c.fecha_vencimiento >= CURRENT_DATE THEN 'Activo' ELSE 'Vencido' END as estado " +
                    "FROM clientes c " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                    "WHERE c.id_usuario = ? AND u.id_empresa = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa);
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
                dto.cancelado = rs.getBoolean("cancelado");
            } else return null;

            /* 2. HISTORIAL DE ASISTENCIAS (AJUSTE HORA ECUADOR) */
            dto.historialAsistencias = new ArrayList<>();
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

            /* 3. RUTINAS DEL DÍA (CORREGIDO PARA MÚLTIPLES RUTINAS)
             * Agrupa TODAS las rutinas activas del cliente y junta sus ejercicios
             */
            dto.ejercicios = new ArrayList<>();
            List<String> nombresRutinas = new ArrayList<>();
            List<String> nombresEntrenadores = new ArrayList<>();

            String sqlRutina = "SELECT r.id_rutina, r.nombre_rutina, COALESCE(e.nombre, 'Staff') as ent " +
                    "FROM rutinas r " +
                    "LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "WHERE r.id_cliente = ? " +
                    "AND r.activa = TRUE " + // Ya no depende de la fecha de creación, solo de que esté activa
                    "ORDER BY r.id_rutina ASC";

            ps = conn.prepareStatement(sqlRutina);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            while(rs.next()){
                nombresRutinas.add(rs.getString("nombre_rutina"));

                String entrenadorActual = rs.getString("ent");
                if (!nombresEntrenadores.contains(entrenadorActual)) {
                    nombresEntrenadores.add(entrenadorActual);
                }

                int idR = rs.getInt("id_rutina");

                // Sub-consulta para extraer los ejercicios de ESTA rutina
                PreparedStatement psEj = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                psEj.setInt(1, idR);
                ResultSet rsEj = psEj.executeQuery();
                while(rsEj.next()) {
                    dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rsEj.getString("nombre_ejercicio"), rsEj.getString("sr")));
                }
            }

            // Si encontró rutinas, une los nombres con una barra "|"
            if (!nombresRutinas.isEmpty()) {
                dto.nombreRutina = String.join(" | ", nombresRutinas);
                dto.entrenador = String.join(", ", nombresEntrenadores);
            } else {
                dto.nombreRutina = null;
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

    /**
     * REGISTRAR ENTRENAMIENTO FINALIZADO (CORREGIDO PARA MÚLTIPLES RUTINAS)
     */
    public boolean registrarTerminoRutina(int idUsuario, int idEmpresa) {
        try (Connection conn = ConexionDB.getConnection()) {
            // Buscamos todas las rutinas activas del cliente
            String sqlInfo = "SELECT r.id_cliente, r.id_rutina FROM rutinas r JOIN clientes c ON r.id_cliente = c.id_cliente JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE u.id_usuario = ? AND u.id_empresa = ? AND r.activa = TRUE";
            PreparedStatement ps = conn.prepareStatement(sqlInfo);
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa);
            ResultSet rs = ps.executeQuery();

            boolean guardado = false;

            // Insertamos un registro de "Terminado" por cada rutina que tenga asignada
            while (rs.next()) {
                String sqlInsert = "INSERT INTO historial_entrenamientos (id_cliente, id_rutina, fecha) " +
                        "SELECT ?, ?, CURRENT_DATE WHERE NOT EXISTS " +
                        "(SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND id_rutina = ? AND fecha = CURRENT_DATE)";

                PreparedStatement psIns = conn.prepareStatement(sqlInsert);
                psIns.setInt(1, rs.getInt("id_cliente"));
                psIns.setInt(2, rs.getInt("id_rutina"));
                psIns.setInt(3, rs.getInt("id_cliente"));
                psIns.setInt(4, rs.getInt("id_rutina"));

                if(psIns.executeUpdate() > 0) guardado = true;
            }
            return guardado;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ==========================================
    // CANCELAR SUSCRIPCIÓN (SOFT DELETE)
    // ==========================================
    public boolean cancelarSuscripcion(int idUsuario, int idEmpresa) {
        String sql = "UPDATE clientes SET cancelado = TRUE WHERE id_usuario = ? AND EXISTS (SELECT 1 FROM usuarios u WHERE u.id_usuario = clientes.id_usuario AND u.id_empresa = ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    // ==========================================
    // REGISTRAR COMPROBANTE DE PAGO (PENDIENTE)
    // ==========================================
    public boolean registrarPagoPendiente(int idCliente, int idMembresia, double monto, int idEmpresa, String base64Imagen) {
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, estado, referencia_comprobante, fecha_pago, id_empresa) " +
                "VALUES (?, ?, ?, 'PENDIENTE', ?, CURRENT_TIMESTAMP, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            ps.setInt(2, idMembresia);
            ps.setDouble(3, monto);
            ps.setString(4, base64Imagen); // Guardamos la imagen convertida en texto
            ps.setInt(5, idEmpresa);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error guardando comprobante: " + e.getMessage());
        }
        return false;
    }
}