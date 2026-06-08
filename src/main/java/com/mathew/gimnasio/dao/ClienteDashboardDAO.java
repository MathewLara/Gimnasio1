/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DEL DASHBOARD DE CLIENTES
 * Componente especializado en la recuperación y consolidación de datos para la vista
 * personalizada de los socios. Se encarga de procesar la telemetría del perfil,
 * el historial de control de accesos, rutinas asignadas y comprobantes de pago.
 */
public class ClienteDashboardDAO {

    // ==========================================
    // 1. OBTENER TELEMETRÍA COMPLETA DEL SOCIO
    // ==========================================

    /**
     * OBTENER INFORMACIÓN DEL DASHBOARD
     * Ejecuta consultas complejas y relacionales para construir un objeto DTO unificado
     * que contiene el perfil del cliente, su estado de membresía, los últimos accesos
     * ajustados a la zona horaria local y las rutinas de entrenamiento vigentes.
     * Parametro idUsuario: Identificador único del cliente.
     * Parametro idEmpresa: Identificador de la sucursal por motivos de validación.
     * Retorna: Objeto ResumenClienteDTO con la telemetría procesada, o null si el usuario no existe.
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

            /* 3. RUTINAS DEL DÍA */
            dto.ejercicios = new ArrayList<>();
            List<String> nombresRutinas = new ArrayList<>();
            List<String> nombresEntrenadores = new ArrayList<>();

            String sqlRutina = "SELECT r.id_rutina, r.nombre_rutina, COALESCE(e.nombre, 'Staff') as ent " +
                    "FROM rutinas r " +
                    "LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador " +
                    "WHERE r.id_cliente = ? AND r.activa = TRUE ORDER BY r.id_rutina ASC";

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
                PreparedStatement psEj = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                psEj.setInt(1, idR);
                ResultSet rsEj = psEj.executeQuery();
                while(rsEj.next()) {
                    dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rsEj.getString("nombre_ejercicio"), rsEj.getString("sr")));
                }
            }

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

    // ==========================================
    // 2. REGISTRAR ENTRENAMIENTO FINALIZADO
    // ==========================================

    /**
     * MARCAR RUTINA COMO TERMINADA
     * Inserta un registro en el historial de entrenamientos certificando que el usuario
     * ha completado su rutina del día, utilizando una validación `NOT EXISTS` a nivel SQL
     * para prevenir duplicidades en la misma jornada.
     * Parametro idUsuario: Identificador del cliente.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Verdadero si se registró el término exitosamente, falso si ya estaba registrada.
     */
    public boolean registrarTerminoRutina(int idUsuario, int idEmpresa) {
        try (Connection conn = ConexionDB.getConnection()) {
            String sqlInfo = "SELECT r.id_cliente, r.id_rutina FROM rutinas r JOIN clientes c ON r.id_cliente = c.id_cliente JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE u.id_usuario = ? AND u.id_empresa = ? AND r.activa = TRUE";
            PreparedStatement ps = conn.prepareStatement(sqlInfo);
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa);
            ResultSet rs = ps.executeQuery();

            boolean guardado = false;
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
    // 3. CANCELAR SUSCRIPCIÓN (SOFT DELETE)
    // ==========================================

    /**
     * CANCELAR SUSCRIPCIÓN DEL CLIENTE
     * Realiza una desactivación lógica (soft-delete) del cliente en el sistema,
     * restringiendo su acceso y evitando que siga acumulando cobros automáticos.
     * Parametro idUsuario: Identificador del cliente.
     * Parametro idEmpresa: Identificador de la sucursal de pertenencia.
     * Retorna: Verdadero si el estado de cancelación se aplicó exitosamente.
     */
    public boolean cancelarSuscripcion(int idUsuario, int idEmpresa) {
        String sql = "UPDATE clientes SET cancelado = TRUE WHERE id_usuario = ? AND EXISTS (SELECT 1 FROM usuarios u WHERE u.id_usuario = clientes.id_usuario AND u.id_empresa = ?)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ==========================================
    // 4. REGISTRAR COMPROBANTE DE PAGO (PENDIENTE)
    // ==========================================

    /**
     * SUBIR COMPROBANTE DE TRANSFERENCIA
     * Almacena de forma persistente la evidencia fotográfica enviada por el cliente
     * en formato Base64, etiquetando el registro transaccional como 'PENDIENTE' para auditoría.
     * Parametro idCliente: Identificador del socio que realiza el pago.
     * Parametro idMembresia: Identificador del plan seleccionado.
     * Parametro monto: Valor económico a revisar.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro base64Imagen: Cadena de texto correspondiente a la imagen del recibo.
     * Retorna: Verdadero si la transacción se guardó correctamente en revisión.
     */
    public boolean registrarPagoPendiente(int idCliente, int idMembresia, double monto, int idEmpresa, String base64Imagen) {
        // La validación de fecha del futuro se previene usando CURRENT_TIMESTAMP del motor de base de datos
        String sql = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, estado, referencia_comprobante, fecha_pago, id_empresa) " +
                "VALUES (?, ?, ?, 'PENDIENTE', ?, CURRENT_TIMESTAMP, ?)";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idMembresia);
            ps.setDouble(3, monto);
            ps.setString(4, base64Imagen);
            ps.setInt(5, idEmpresa);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ==========================================
    // VALIDACIONES DE SEGURIDAD (NUEVO)
    // ==========================================

    /**
     * VALIDAR PERTENENCIA A SUCURSAL
     * Capa de seguridad Multi-Tenant que verifica que las solicitudes y cobros
     * de un cliente correspondan exclusivamente a la base de datos de su gimnasio matriculado.
     * Parametro idCliente: Identificador del socio.
     * Parametro idEmpresa: Identificador de la sucursal actual.
     * Retorna: Verdadero si la relación entre cliente y empresa es legítima.
     */
    public boolean existeUsuarioEnEmpresa(int idCliente, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM clientes c INNER JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE c.id_cliente = ? AND u.id_empresa = ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * VALIDAR DISPONIBILIDAD DE MEMBRESÍA
     * Comprueba que el plan que el usuario está intentando pagar exista,
     * esté activo y forme parte del catálogo de su sucursal asignada.
     * Parametro idMembresia: Identificador del plan.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Verdadero si el plan es elegible para compra.
     */
    public boolean existeMembresiaEnEmpresa(int idMembresia, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM membresias WHERE id_membresia = ? AND id_empresa = ? AND activa = TRUE";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idMembresia);
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /**
     * PREVENCIÓN DE DUPLICIDAD DE COMPROBANTES (ANTI-SPAM)
     * Verifica en la base de datos si el usuario ya mantiene un comprobante
     * en cola de revisión para evitar congestión en el panel de recepción.
     * Parametro idCliente: Identificador del socio evaluado.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Verdadero si existe al menos un comprobante pendiente de resolución.
     */
    public boolean tienePagoPendiente(int idCliente, int idEmpresa) {
        String sql = "SELECT COUNT(*) FROM pagos WHERE id_cliente = ? AND id_empresa = ? AND estado = 'PENDIENTE'";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}