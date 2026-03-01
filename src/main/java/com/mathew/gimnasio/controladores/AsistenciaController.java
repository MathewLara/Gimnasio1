package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.AccesoManualDTO;
import com.mathew.gimnasio.util.JsonUtil;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * CONTROLADOR DE ASISTENCIAS
 * Gestiona el flujo de entradas y salidas de los clientes al gimnasio.
 * Se activa cuando un cliente escanea su código QR en la recepción.
 */
@Path("/accesos")
public class AsistenciaController {

    // ================================================================
    // ENDPOINT ORIGINAL — Se mantiene intacto
    // ================================================================

    /**
     * PROCESAR ACCESO (ENTRADA/SALIDA) — RF06
     * Funciona de forma inteligente: si el cliente no ha entrado hoy, marca ENTRADA.
     * Si ya entró pero no ha salido, marca SALIDA.
     * AHORA también valida membresía y devuelve alertas.
     *
     * URL: POST /api/accesos/escanear/{idUsuario}
     */
    @POST
    @Path("/escanear/{idUsuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarAcceso(@PathParam("idUsuario") int idUsuario) {
        String mensaje = "";
        String tipo = "";
        String alerta = "";
        boolean puedeEntrar = true;

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. TRADUCCIÓN DE ID + DATOS DE MEMBRESÍA
            // Ahora también traemos el estado de membresía para validar el acceso
            String sqlCliente =
                    "SELECT c.id_cliente, c.nombre, c.fecha_vencimiento, " +
                            "CASE WHEN c.fecha_vencimiento >= CURRENT_DATE THEN 'Activo' ELSE 'Vencido' END as estado, " +
                            "CURRENT_DATE - c.fecha_vencimiento as dias_vencido, " +
                            "c.fecha_vencimiento - CURRENT_DATE as dias_restantes " +
                            "FROM clientes c WHERE c.id_usuario = ?";

            PreparedStatement psCl = conn.prepareStatement(sqlCliente);
            psCl.setInt(1, idUsuario);
            ResultSet rsCl = psCl.executeQuery();

            int idCliente = 0;
            String nombre = "";

            if (rsCl.next()) {
                idCliente = rsCl.getInt("id_cliente");
                nombre = rsCl.getString("nombre");
                String estadoMembresia = rsCl.getString("estado");
                int diasRestantes = rsCl.getInt("dias_restantes");
                int diasVencido = rsCl.getInt("dias_vencido");

                // VALIDACIÓN DE MEMBRESÍA: Si está vencida, bloqueamos la entrada
                if ("Vencido".equals(estadoMembresia)) {
                    puedeEntrar = false;
                    return Response.status(403).entity(
                            "{\"mensaje\": \"❌ Acceso denegado, " + nombre + ". Tu membresía venció hace " + diasVencido + " día(s).\", " +
                                    "\"tipo\": \"DENEGADO\", " +
                                    "\"puedeEntrar\": false, " +
                                    "\"alerta\": \"Membresía vencida. Dirígete a recepción para renovar.\"}"
                    ).build();
                }

                // ALERTA: Membresía por vencer en los próximos 5 días
                if (diasRestantes >= 0 && diasRestantes <= 5) {
                    alerta = "⚠️ Tu membresía vence en " + diasRestantes + " día(s). ¡Renueva pronto!";
                }

            } else {
                return Response.status(404)
                        .entity("{\"mensaje\": \"Usuario no encontrado en clientes\"}")
                        .build();
            }

            // 2. VERIFICAR ESTADO ACTUAL (¿ya tiene entrada abierta hoy?)
            String sqlCheck =
                    "SELECT id_asistencia FROM asistencias " +
                            "WHERE id_cliente = ? AND fecha_hora_salida IS NULL " +
                            "AND DATE(fecha_hora_ingreso) = CURRENT_DATE " +
                            "ORDER BY id_asistencia DESC LIMIT 1";

            PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, idCliente);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                // CASO A: MARCAR SALIDA
                int idAsistencia = rs.getInt("id_asistencia");
                String sqlSalida =
                        "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP " +
                                "WHERE id_asistencia = ?";
                PreparedStatement psUpd = conn.prepareStatement(sqlSalida);
                psUpd.setInt(1, idAsistencia);
                psUpd.executeUpdate();

                mensaje = "👋 ¡Hasta luego, " + nombre + "!";
                tipo = "SALIDA";
            } else {
                // CASO B: MARCAR ENTRADA
                String sqlEntrada =
                        "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso) " +
                                "VALUES (?, CURRENT_TIMESTAMP)";
                PreparedStatement psIns = conn.prepareStatement(sqlEntrada);
                psIns.setInt(1, idCliente);
                psIns.executeUpdate();

                mensaje = "🚀 ¡Bienvenido, " + nombre + "!";
                tipo = "ENTRADA";
            }

            // Construimos la respuesta con alerta incluida (puede ser vacía)
            String alertaJson = alerta.isEmpty() ? "null" : "\"" + alerta + "\"";
            return Response.ok(
                    "{\"mensaje\": \"" + mensaje + "\", " +
                            "\"tipo\": \"" + tipo + "\", " +
                            "\"puedeEntrar\": true, " +
                            "\"alerta\": " + alertaJson + "}"
            ).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity("{\"mensaje\": \"Error interno\"}")
                    .build();
        }
    }

    // ================================================================
    // CRÍTICO 1 — GET /api/accesos/estado/{idUsuario}
    // ================================================================

    /**
     * VERIFICAR ESTADO DEL CLIENTE ANTES DE ENTRAR — RF06 + RF03
     *
     * Devuelve nombre, estado de membresía, días restantes y si puede entrar.
     * Útil para que la pantalla del escáner muestre una previsualización
     * antes de confirmar el acceso (ej: tarjeta de bienvenida o alerta de bloqueo).
     *
     * URL: GET /api/accesos/estado/{idUsuario}
     *
     * Respuesta ejemplo:
     * {
     *   "nombre": "Juan Pérez",
     *   "estadoMembresia": "Activo",
     *   "plan": "Black",
     *   "fechaVencimiento": "2026-03-15",
     *   "diasRestantes": 15,
     *   "puedeEntrar": true,
     *   "dentroPorDia": false,
     *   "alerta": null
     * }
     */
    @GET
    @Path("/estado/{idUsuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarEstado(@PathParam("idUsuario") int idUsuario) {
        try (Connection conn = ConexionDB.getConnection()) {

            // Traemos perfil completo + membresía del cliente
            String sql =
                    "SELECT c.id_cliente, c.nombre || ' ' || c.apellido as nombre_completo, " +
                            "m.nombre as plan, c.fecha_vencimiento, " +
                            "CASE WHEN c.fecha_vencimiento >= CURRENT_DATE THEN 'Activo' ELSE 'Vencido' END as estado, " +
                            "c.fecha_vencimiento - CURRENT_DATE as dias_restantes " +
                            "FROM clientes c " +
                            "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                            "WHERE c.id_usuario = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return Response.status(404)
                        .entity("{\"mensaje\": \"Cliente no encontrado\"}")
                        .build();
            }

            int idCliente      = rs.getInt("id_cliente");
            String nombre      = rs.getString("nombre_completo");
            String plan        = rs.getString("plan") != null ? rs.getString("plan") : "Sin Membresía";
            String fechaVenc   = rs.getString("fecha_vencimiento");
            String estado      = rs.getString("estado");
            int diasRestantes  = rs.getInt("dias_restantes");
            boolean puedeEntrar = "Activo".equals(estado);

            // ¿Ya está dentro hoy? (entrada sin salida)
            PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT 1 FROM asistencias " +
                            "WHERE id_cliente = ? AND fecha_hora_salida IS NULL " +
                            "AND DATE(fecha_hora_ingreso) = CURRENT_DATE LIMIT 1"
            );
            psCheck.setInt(1, idCliente);
            ResultSet rsCheck = psCheck.executeQuery();
            boolean dentroPorDia = rsCheck.next();

            // Construir alerta según contexto
            String alerta = "null";
            if (!puedeEntrar) {
                alerta = "\"Membresía vencida. Renovar en recepción.\"";
            } else if (diasRestantes <= 5) {
                alerta = "\"⚠️ Membresía vence en " + diasRestantes + " día(s)\"";
            }

            return Response.ok(
                    "{" +
                            "\"nombre\": \"" + nombre + "\", " +
                            "\"estadoMembresia\": \"" + estado + "\", " +
                            "\"plan\": \"" + plan + "\", " +
                            "\"fechaVencimiento\": \"" + (fechaVenc != null ? fechaVenc : "") + "\", " +
                            "\"diasRestantes\": " + diasRestantes + ", " +
                            "\"puedeEntrar\": " + puedeEntrar + ", " +
                            "\"dentroPorDia\": " + dentroPorDia + ", " +
                            "\"alerta\": " + alerta +
                            "}"
            ).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity("{\"mensaje\": \"Error interno\"}")
                    .build();
        }
    }

    // ================================================================
    // CRÍTICO 2 — GET /api/accesos/presentes
    // ================================================================

    /**
     * CLIENTES PRESENTES AHORA — RF06
     *
     * Lista todos los clientes que tienen una entrada sin salida en el día actual.
     * Ideal para la pantalla de recepción que muestra quién está dentro del gimnasio
     * en tiempo real.
     *
     * URL: GET /api/accesos/presentes
     *
     * Respuesta ejemplo:
     * [
     *   {
     *     "idAsistencia": 45,
     *     "idCliente": 12,
     *     "nombre": "Juan Pérez",
     *     "plan": "Black",
     *     "horaIngreso": "08:30",
     *     "minutosEnGimnasio": 47
     *   }
     * ]
     */
    @GET
    @Path("/presentes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPresentes() {
        StringBuilder json = new StringBuilder("[");

        try (Connection conn = ConexionDB.getConnection()) {

            String sql =
                    "SELECT a.id_asistencia, a.id_cliente, " +
                            "c.nombre || ' ' || c.apellido as nombre_completo, " +
                            "COALESCE(m.nombre, 'Sin Plan') as plan, " +
                            "to_char(a.fecha_hora_ingreso, 'HH24:MI') as hora_ingreso, " +
                            // Minutos que lleva en el gimnasio
                            "EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - a.fecha_hora_ingreso)) / 60 as minutos " +
                            "FROM asistencias a " +
                            "JOIN clientes c ON a.id_cliente = c.id_cliente " +
                            "LEFT JOIN membresias m ON c.id_membresia = m.id_membresia " +
                            "WHERE a.fecha_hora_salida IS NULL " +
                            "AND DATE(a.fecha_hora_ingreso) = CURRENT_DATE " +
                            "ORDER BY a.fecha_hora_ingreso ASC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            boolean first = true;
            int totalPresentes = 0;

            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"idAsistencia\": ").append(rs.getInt("id_asistencia")).append(", ")
                        .append("\"idCliente\": ").append(rs.getInt("id_cliente")).append(", ")
                        .append("\"nombre\": \"").append(JsonUtil.escape(rs.getString("nombre_completo"))).append("\", ")
                        .append("\"plan\": \"").append(JsonUtil.escape(rs.getString("plan"))).append("\", ")
                        .append("\"horaIngreso\": \"").append(rs.getString("hora_ingreso")).append("\", ")
                        .append("\"minutosEnGimnasio\": ").append((int) rs.getDouble("minutos"))
                        .append("}");
                first = false;
                totalPresentes++;
            }

            json.append("]");

            // Envolvemos en objeto con metadatos útiles para recepción
            return Response.ok(
                    "{\"total\": " + totalPresentes + ", \"clientes\": " + json + "}"
            ).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity("{\"mensaje\": \"Error interno\"}")
                    .build();
        }
    }

    // ================================================================
    // CRÍTICO 3 — POST /api/accesos/manual
    // ================================================================

    /**
     * REGISTRO MANUAL DE ENTRADA/SALIDA — RF06
     *
     * Permite al recepcionista registrar un acceso manualmente cuando:
     * - El cliente olvidó el teléfono
     * - El QR está dañado o no funciona
     * - Se necesita corregir un error del día
     *
     * URL: POST /api/accesos/manual
     *
     * Body esperado:
     * {
     *   "idCliente": 12,
     *   "tipo": "ENTRADA",          // o "SALIDA"
     *   "motivo": "QR dañado"       // texto libre, opcional
     * }
     *
     * Respuesta ejemplo:
     * {
     *   "mensaje": "✅ ENTRADA manual registrada para Juan Pérez",
     *   "tipo": "ENTRADA",
     *   "idAsistencia": 88
     * }
     */
    @POST
    @Path("/manual")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarAccesoManual(AccesoManualDTO dto) {

        // Validaciones básicas del body
        if (dto == null || dto.getIdCliente() <= 0) {
            return Response.status(400)
                    .entity("{\"mensaje\": \"idCliente es obligatorio\"}")
                    .build();
        }

        String tipo = dto.getTipo();
        if (tipo == null || (!tipo.equalsIgnoreCase("ENTRADA") && !tipo.equalsIgnoreCase("SALIDA"))) {
            return Response.status(400)
                    .entity("{\"mensaje\": \"tipo debe ser ENTRADA o SALIDA\"}")
                    .build();
        }

        try (Connection conn = ConexionDB.getConnection()) {

            // Verificar que el cliente existe y traer su nombre
            PreparedStatement psNombre = conn.prepareStatement(
                    "SELECT nombre || ' ' || apellido as nombre_completo FROM clientes WHERE id_cliente = ?"
            );
            psNombre.setInt(1, dto.getIdCliente());
            ResultSet rsNombre = psNombre.executeQuery();

            if (!rsNombre.next()) {
                return Response.status(404)
                        .entity("{\"mensaje\": \"Cliente no encontrado\"}")
                        .build();
            }

            String nombre = rsNombre.getString("nombre_completo");
            int idAsistenciaResultante = 0;
            String motivo = dto.getMotivo() != null ? dto.getMotivo() : "Registro manual";

            if (tipo.equalsIgnoreCase("ENTRADA")) {

                // Verificar que no tenga ya una entrada abierta hoy
                PreparedStatement psCheck = conn.prepareStatement(
                        "SELECT id_asistencia FROM asistencias " +
                                "WHERE id_cliente = ? AND fecha_hora_salida IS NULL " +
                                "AND DATE(fecha_hora_ingreso) = CURRENT_DATE LIMIT 1"
                );
                psCheck.setInt(1, dto.getIdCliente());
                ResultSet rsCheck = psCheck.executeQuery();

                if (rsCheck.next()) {
                    // Ya tiene entrada abierta, no creamos duplicado
                    return Response.status(409).entity(
                            "{\"mensaje\": \"" + nombre + " ya tiene una entrada abierta hoy. " +
                                    "Si desea marcar salida, use tipo: SALIDA\", " +
                                    "\"tipo\": \"DUPLICADO\"}"
                    ).build();
                }

                // Insertar entrada manual (con observacion si existe columna; ver migracion_rf.sql)
                try {
                    PreparedStatement psIns = conn.prepareStatement(
                            "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso, observacion) " +
                                    "VALUES (?, CURRENT_TIMESTAMP, ?) RETURNING id_asistencia");
                    psIns.setInt(1, dto.getIdCliente());
                    psIns.setString(2, "[MANUAL] " + motivo);
                    ResultSet rsGen = psIns.executeQuery();
                    if (rsGen.next()) idAsistenciaResultante = rsGen.getInt(1);
                } catch (Exception ex) {
                    PreparedStatement psIns = conn.prepareStatement(
                            "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso) " +
                                    "VALUES (?, CURRENT_TIMESTAMP) RETURNING id_asistencia");
                    psIns.setInt(1, dto.getIdCliente());
                    ResultSet rsGen = psIns.executeQuery();
                    if (rsGen.next()) idAsistenciaResultante = rsGen.getInt(1);
                }

            } else {
                // SALIDA MANUAL: buscamos la entrada abierta de hoy y la cerramos
                PreparedStatement psCheck = conn.prepareStatement(
                        "SELECT id_asistencia FROM asistencias " +
                                "WHERE id_cliente = ? AND fecha_hora_salida IS NULL " +
                                "AND DATE(fecha_hora_ingreso) = CURRENT_DATE " +
                                "ORDER BY id_asistencia DESC LIMIT 1"
                );
                psCheck.setInt(1, dto.getIdCliente());
                ResultSet rsCheck = psCheck.executeQuery();

                if (!rsCheck.next()) {
                    return Response.status(404).entity(
                            "{\"mensaje\": \"No hay entrada abierta hoy para " + nombre + "\"}"
                    ).build();
                }

                idAsistenciaResultante = rsCheck.getInt("id_asistencia");

                PreparedStatement psUpd = conn.prepareStatement(
                        "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP " +
                                "WHERE id_asistencia = ?"
                );
                psUpd.setInt(1, idAsistenciaResultante);
                psUpd.executeUpdate();
            }

            String emoji = tipo.equalsIgnoreCase("ENTRADA") ? "✅" : "🚪";
            return Response.ok(
                    "{\"mensaje\": \"" + emoji + " " + tipo.toUpperCase() + " manual registrada para " + nombre + "\", " +
                            "\"tipo\": \"" + tipo.toUpperCase() + "\", " +
                            "\"idAsistencia\": " + idAsistenciaResultante + ", " +
                            "\"motivo\": \"" + JsonUtil.escape(motivo) + "\"}"
            ).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity("{\"mensaje\": \"Error interno\"}")
                    .build();
        }
    }

    // ================================================================
    // IMPORTANTE — GET /api/accesos/historial/{idCliente}
    // ================================================================

    /**
     * HISTORIAL DE ACCESOS POR CLIENTE
     * Lista entradas/salidas del cliente para recepción o admin.
     * URL: GET /api/accesos/historial/{idCliente}
     * Query: ?limite=50 (opcional)
     */
    @GET
    @Path("/historial/{idCliente}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHistorialCliente(@PathParam("idCliente") int idCliente,
                                        @QueryParam("limite") Integer limite) {
        int lim = (limite != null && limite > 0) ? Math.min(limite, 200) : 50;
        StringBuilder json = new StringBuilder("[");

        try (Connection conn = ConexionDB.getConnection()) {
            String sql =
                    "SELECT a.id_asistencia, a.fecha_hora_ingreso, a.fecha_hora_salida, " +
                            "to_char(a.fecha_hora_ingreso, 'YYYY-MM-DD') as dia, " +
                            "to_char(a.fecha_hora_ingreso, 'HH24:MI') as hora_entrada, " +
                            "to_char(a.fecha_hora_salida, 'HH24:MI') as hora_salida " +
                            "FROM asistencias a WHERE a.id_cliente = ? " +
                            "ORDER BY a.fecha_hora_ingreso DESC LIMIT ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idCliente);
            ps.setInt(2, lim);
            ResultSet rs = ps.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                String horaSalida = rs.getString("hora_salida");
                json.append("{")
                        .append("\"idAsistencia\":").append(rs.getInt("id_asistencia")).append(",")
                        .append("\"dia\":\"").append(rs.getString("dia")).append("\",")
                        .append("\"horaEntrada\":\"").append(rs.getString("hora_entrada")).append("\",")
                        .append("\"horaSalida\":").append(horaSalida != null ? "\"" + horaSalida + "\"" : "null").append(",")
                        .append("\"fechaHoraIngreso\":\"").append(rs.getTimestamp("fecha_hora_ingreso")).append("\",")
                        .append("\"fechaHoraSalida\":").append(rs.getTimestamp("fecha_hora_salida") != null ? "\"" + rs.getTimestamp("fecha_hora_salida") + "\"" : "null")
                        .append("}");
                first = false;
            }
            json.append("]");
            return Response.ok(json.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }

    // ================================================================
    // IMPORTANTE — GET /api/accesos/reporte-dia
    // ================================================================

    /**
     * REPORTE DEL DÍA — Cierre de turno recepción
     * Total entradas, salidas, presentes y listado de movimientos del día.
     * URL: GET /api/accesos/reporte-dia
     * Query: ?fecha=YYYY-MM-DD (opcional, por defecto hoy)
     */
    @GET
    @Path("/reporte-dia")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReporteDia(@QueryParam("fecha") String fechaParam) {
        try (Connection conn = ConexionDB.getConnection()) {
            java.sql.Date fecha = java.sql.Date.valueOf(
                    (fechaParam != null && !fechaParam.isEmpty()) ? fechaParam : java.time.LocalDate.now().toString());

            // Totales del día
            PreparedStatement psTot = conn.prepareStatement(
                    "SELECT " +
                            "COUNT(*) as total_entradas, " +
                            "COUNT(fecha_hora_salida) as total_salidas, " +
                            "COUNT(*) - COUNT(fecha_hora_salida) as abiertas " +
                            "FROM asistencias WHERE DATE(fecha_hora_ingreso) = ?");
            psTot.setDate(1, fecha);
            ResultSet rsTot = psTot.executeQuery();
            int totalEntradas = 0, totalSalidas = 0, abiertas = 0;
            if (rsTot.next()) {
                totalEntradas = rsTot.getInt("total_entradas");
                totalSalidas = rsTot.getInt("total_salidas");
                abiertas = rsTot.getInt("abiertas");
            }

            // Listado de movimientos del día
            StringBuilder movimientos = new StringBuilder("[");
            PreparedStatement psMov = conn.prepareStatement(
                    "SELECT a.id_asistencia, c.nombre || ' ' || c.apellido as nombre, " +
                            "to_char(a.fecha_hora_ingreso, 'HH24:MI') as entrada, " +
                            "to_char(a.fecha_hora_salida, 'HH24:MI') as salida " +
                            "FROM asistencias a JOIN clientes c ON a.id_cliente = c.id_cliente " +
                            "WHERE DATE(a.fecha_hora_ingreso) = ? ORDER BY a.fecha_hora_ingreso");
            psMov.setDate(1, fecha);
            ResultSet rsMov = psMov.executeQuery();
            boolean first = true;
            while (rsMov.next()) {
                if (!first) movimientos.append(",");
                String sal = rsMov.getString("salida");
                movimientos.append("{\"idAsistencia\":").append(rsMov.getInt("id_asistencia"))
                        .append(",\"nombre\":\"").append(JsonUtil.escape(rsMov.getString("nombre")))
                        .append("\",\"entrada\":\"").append(rsMov.getString("entrada"))
                        .append("\",\"salida\":").append(sal != null ? "\"" + sal + "\"" : "null").append("}");
                first = false;
            }
            movimientos.append("]");

            String json = "{\"fecha\":\"" + fecha + "\"," +
                    "\"totalEntradas\":" + totalEntradas + "," +
                    "\"totalSalidas\":" + totalSalidas + "," +
                    "\"entradasSinCerrar\":" + abiertas + "," +
                    "\"movimientos\":" + movimientos + "}";
            return Response.ok(json).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }

    // ================================================================
    // IMPORTANTE — PUT /api/accesos/{id}/cerrar
    // ================================================================

    /**
     * CERRAR ENTRADA HUÉRFANA
     * Marca fecha_hora_salida en una asistencia que quedó abierta.
     * URL: PUT /api/accesos/{id}/cerrar
     */
    @PUT
    @Path("/{idAsistencia}/cerrar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cerrarAsistencia(@PathParam("idAsistencia") int idAsistencia) {
        try (Connection conn = ConexionDB.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP " +
                            "WHERE id_asistencia = ? AND fecha_hora_salida IS NULL RETURNING id_asistencia");
            ps.setInt(1, idAsistencia);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Response.ok("{\"mensaje\": \"Entrada cerrada correctamente\", \"idAsistencia\": " + idAsistencia + "}").build();
            }
            return Response.status(404).entity(
                    "{\"mensaje\": \"Asistencia no encontrada o ya estaba cerrada\"}"
            ).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }

    // ================================================================
    // DESEABLE — GET /api/accesos/estadisticas
    // ================================================================

    /**
     * ESTADÍSTICAS DE ACCESOS — Dashboard admin
     * Totales del día y resumen para pantalla de administración.
     * URL: GET /api/accesos/estadisticas
     */
    @GET
    @Path("/estadisticas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEstadisticas() {
        try (Connection conn = ConexionDB.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT " +
                            "(SELECT COUNT(*) FROM asistencias WHERE DATE(fecha_hora_ingreso) = CURRENT_DATE) as entradas_hoy, " +
                            "(SELECT COUNT(*) FROM asistencias WHERE fecha_hora_salida IS NULL AND DATE(fecha_hora_ingreso) = CURRENT_DATE) as presentes_ahora, " +
                            "(SELECT COUNT(DISTINCT id_cliente) FROM asistencias WHERE DATE(fecha_hora_ingreso) = CURRENT_DATE) as clientes_unicos_hoy, " +
                            "(SELECT COUNT(*) FROM asistencias WHERE DATE(fecha_hora_ingreso) BETWEEN CURRENT_DATE - 7 AND CURRENT_DATE) as entradas_ultima_semana"
            );
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String json = "{" +
                        "\"entradasHoy\":" + rs.getInt("entradas_hoy") + "," +
                        "\"presentesAhora\":" + rs.getInt("presentes_ahora") + "," +
                        "\"clientesUnicosHoy\":" + rs.getInt("clientes_unicos_hoy") + "," +
                        "\"entradasUltimaSemana\":" + rs.getInt("entradas_ultima_semana") + "}";
                return Response.ok(json).build();
            }
            return Response.ok("{\"entradasHoy\":0,\"presentesAhora\":0,\"clientesUnicosHoy\":0,\"entradasUltimaSemana\":0}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }

    // ================================================================
    // DESEABLE — GET /api/accesos/qr-token/{idUsuario}
    // ================================================================

    /**
     * QR CON TOKEN SEGURO
     * Devuelve un payload para el QR del cliente (idUsuario): identificador + token corto
     * para que el kiosco valide que el QR no fue adulterado.
     * URL: GET /api/accesos/qr-token/{idUsuario}
     */
    @GET
    @Path("/qr-token/{idUsuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQrToken(@PathParam("idUsuario") int idUsuario) {
        try (Connection conn = ConexionDB.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.id_cliente FROM clientes c WHERE c.id_usuario = ?");
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return Response.status(404).entity("{\"mensaje\": \"Cliente no encontrado\"}").build();
            }
            int idCliente = rs.getInt("id_cliente");
            // Token corto: HMAC o JWT de corta duración (ej. 5 min). Usamos JwtService si está disponible.
            String token;
            try {
                token = com.mathew.gimnasio.util.JwtService.generarToken(idUsuario, 4, "qr");
                // JWT por defecto dura 24h; para QR podemos usar el mismo (el kiosco valida con el mismo secret)
            } catch (Exception e) {
                token = "IRON_" + idUsuario + "_" + System.currentTimeMillis();
            }
            String payload = "{\"idUsuario\":" + idUsuario + ",\"idCliente\":" + idCliente + ",\"token\":\"" + token + "\"}";
            return Response.ok(payload).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }

}