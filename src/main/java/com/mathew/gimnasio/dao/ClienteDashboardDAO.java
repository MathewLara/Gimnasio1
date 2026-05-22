package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

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
    // SE AÑADIÓ idEmpresa
    public ResumenClienteDTO obtenerInfoDashboard(int idUsuario, int idEmpresa) {
        ResumenClienteDTO dto = new ResumenClienteDTO();

        try (Connection conn = ConexionDB.getConnection()) {

            /* 1. PERFIL Y ESTADO DE MEMBRESÍA
             * Cruce relacional (LEFT JOIN) para obtener los datos del cliente y su plan financiero.
             * Utiliza lógica condicional en SQL (CASE WHEN) para evaluar si el plan está vencido
             * basándose en la fecha del servidor (CURRENT_DATE).
             */
            // SE AÑADIÓ JOIN u Y LA CONDICIÓN DE id_empresa
            String sql = "SELECT c.id_cliente, c.nombre || ' ' || c.apellido as n, c.email, c.telefono, " +
                    "m.nombre as plan, m.precio, c.fecha_vencimiento, c.cancelado, " +
                    "CASE WHEN c.fecha_vencimiento >= CURRENT_DATE THEN 'Activo' ELSE 'Vencido' END as estado " +
                    "FROM clientes c " +
                    "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                    "JOIN usuarios u ON c.id_usuario = u.id_usuario " +
                    "WHERE c.id_usuario = ? AND u.id_empresa = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa); // INYECTANDO LA EMPRESA
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
            } else return null; // Aborta si el usuario no tiene un perfil de cliente asociado o no pertenece a la empresa

            /* 2. HISTORIAL DE ASISTENCIAS (AJUSTE HORA ECUADOR)
             * Extrae los últimos 5 ingresos/salidas del cliente.
             * Ajuste Arquitectónico: Se utiliza INTERVAL '5 hours' directamente en PostgreSQL
             * para convertir la zona horaria (UTC a GMT-5) y asegurar que el frontend muestre la hora real.
             */
            dto.historialAsistencias = new ArrayList<>();
            // Restamos 5 hours a la entrada y a la salida para que coincida con Ecuador
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

                // Capturamos el registro más reciente para los KPIs superiores del Dashboard
                if (primeraFila) {
                    dto.ultimoIngreso = horaIn;
                    dto.ultimaSalida = horaOut;
                    primeraFila = false;
                }
            }

            /* 3. RUTINA DEL DÍA (MANTENIENDO TU LÓGICA)
             * Busca la rutina asignada para el día actual y extrae sus ejercicios en una sub-consulta.
             */
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

                // Sub-consulta para extraer los detalles (series y repeticiones)
                PreparedStatement psEj = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                psEj.setInt(1, idR);
                ResultSet rsEj = psEj.executeQuery();
                while(rsEj.next()) {
                    dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rsEj.getString("nombre_ejercicio"), rsEj.getString("sr")));
                }
            }

            /* 4. VERIFICACIÓN DE RUTINA TERMINADA
             * Consulta de existencia ultrarrápida. Retorna true si hay un log de entrenamiento hoy.
             */
            String sqlCheck = "SELECT 1 FROM historial_entrenamientos WHERE id_cliente = ? AND fecha = CURRENT_DATE";
            ps = conn.prepareStatement(sqlCheck);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();
            dto.rutinaTerminadaHoy = rs.next();

        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }

    /* MANTENIENDO TU FUNCIÓN DE REGISTRO DE TÉRMINO */
    /**
     * REGISTRAR ENTRENAMIENTO FINALIZADO
     * Inserta un registro en el historial para gamificación y control del entrenador.
     * Implementa protección anti-duplicados usando "WHERE NOT EXISTS" directamente en SQL.
     */
    // SE AÑADIÓ idEmpresa
    public boolean registrarTerminoRutina(int idUsuario, int idEmpresa) {
        try (Connection conn = ConexionDB.getConnection()) {
            // SE AÑADIÓ JOIN u Y LA CONDICIÓN DE id_empresa PARA AISLAMIENTO
            String sqlInfo = "SELECT r.id_cliente, r.id_rutina FROM rutinas r JOIN clientes c ON r.id_cliente = c.id_cliente JOIN usuarios u ON c.id_usuario = u.id_usuario WHERE u.id_usuario = ? AND u.id_empresa = ? ORDER BY r.id_rutina DESC LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(sqlInfo);
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa); // INYECTANDO LA EMPRESA
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
    // CANCELAR SUSCRIPCIÓN (CORREGIDO)
    // ==========================================
    /**
     * CANCELAR SUSCRIPCIÓN (SOFT DELETE / FLAG)
     * No elimina al usuario, solo enciende una bandera (cancelado = TRUE)
     * para cortar el acceso a los servicios físicos y lógicos.
     */
    // SE AÑADIÓ idEmpresa
    public boolean cancelarSuscripcion(int idUsuario, int idEmpresa) {
        // SE AÑADIÓ LA VERIFICACIÓN EXISTS PARA GARANTIZAR QUE EL USUARIO PERTENECE A LA EMPRESA
        String sql = "UPDATE clientes SET cancelado = TRUE WHERE id_usuario = ? AND EXISTS (SELECT 1 FROM usuarios u WHERE u.id_usuario = clientes.id_usuario AND u.id_empresa = ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idEmpresa); // INYECTANDO LA EMPRESA
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}