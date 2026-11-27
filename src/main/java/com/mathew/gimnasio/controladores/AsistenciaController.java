/**
 * Author: Mathew Lara
 * Fecha: 07/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.configuracion.ConexionDB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * CONTROLADOR DE ASISTENCIAS (HARDWARE INTEGRATION)
 * Este controlador gestiona el flujo de control de acceso físico al gimnasio.
 * Es invocado de forma asíncrona cada vez que el componente Html5Qrcode
 * lee un código válido en la recepción o en el kiosko.
 */
@Path("/accesos")
public class AsistenciaController {

    /**
     * PROCESAR ACCESO INTELIGENTE (TOGGLE ENTRADA/SALIDA)
     * Este metodo contiene lógica de negocio transaccional: detecta automáticamente
     * si el usuario está ingresando o abandonando las instalaciones.
     * URL: POST /api/accesos/escanear/{idUsuario}
     * Parámetro idUsuario: El ID decodificado del código QR.
     * Retorna: Respuesta JSON con un mensaje dinámico renderizable en la UI.
     */
    @POST
    @Path("/escanear/{idUsuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarAcceso(@PathParam("idUsuario") int idUsuario) {
        String mensaje = "";
        String tipo = "";

        // Patrón Try-with-resources: Garantiza el cierre de la conexión JDBC evitando fugas de memoria
        try (Connection conn = ConexionDB.getConnection()) {

            /* 1. TRADUCCIÓN DE CLAVES FORÁNEAS (USER -> CLIENTE)
             * El código QR entrega el 'id_usuario' de autenticación, pero el log de asistencia
             * requiere el 'id_cliente' del negocio. Resolvemos la relación.
             */
            String sqlCliente = "SELECT id_cliente, nombre FROM clientes WHERE id_usuario = ?";
            PreparedStatement psCl = conn.prepareStatement(sqlCliente);
            psCl.setInt(1, idUsuario);
            ResultSet rsCl = psCl.executeQuery();

            int idCliente = 0;
            String nombre = "";

            if (rsCl.next()) {
                idCliente = rsCl.getInt("id_cliente");
                nombre = rsCl.getString("nombre");
            } else {
                // HTTP 404: Intercepta falsificaciones de QR o usuarios sin perfil de cliente
                return Response.status(404).entity("{\"mensaje\": \"Usuario no encontrado en clientes\"}").build();
            }

            /* 2. VERIFICAR ESTADO DE SESIÓN FÍSICA (TOGGLE LOGIC)
             * Busca si existe un registro de entrada "abierto" (sin salida) para la fecha de HOY.
             */
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_cliente = ? AND fecha_hora_salida IS NULL AND DATE(fecha_hora_ingreso) = CURRENT_DATE ORDER BY id_asistencia DESC LIMIT 1";
            PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, idCliente);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                /* * CASO A: MARCAR SALIDA (CHECK-OUT)
                 * Se encontró un registro abierto. Se actualiza inyectando el TIMESTAMP actual de PostgreSQL.
                 */
                int idAsistencia = rs.getInt("id_asistencia");
                String sqlSalida = "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP WHERE id_asistencia = ?";
                PreparedStatement psUpd = conn.prepareStatement(sqlSalida);
                psUpd.setInt(1, idAsistencia);
                psUpd.executeUpdate();

                mensaje = " ¡Hasta luego, " + nombre + "!";
                tipo = "SALIDA";
            } else {
                /* * CASO B: MARCAR ENTRADA (CHECK-IN)
                 * No hay registros abiertos hoy. Se inserta una nueva fila delegando la fecha/hora al motor DB.
                 */
                String sqlEntrada = "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso) VALUES (?, CURRENT_TIMESTAMP)";
                PreparedStatement psIns = conn.prepareStatement(sqlEntrada);
                psIns.setInt(1, idCliente);
                psIns.executeUpdate();

                mensaje = " ¡Bienvenido, " + nombre + "!";
                tipo = "ENTRADA";
            }

            // 3. Serialización manual segura para enviar el feedback visual al escáner
            return Response.ok("{\"mensaje\": \"" + mensaje + "\", \"tipo\": \"" + tipo + "\"}").build();

        } catch (Exception e) {
            e.printStackTrace(); // Log del servidor para auditoría
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build(); // HTTP 500
        }
    }
    /**
     * REGISTRO DE ASISTENCIA POR RECONOCIMIENTO FACIAL (APK / FastAPI)
     * URL: POST /Gimnasio/api/accesos/facial/{idCliente}
     * Retorna JSON con estado: ACCEPTED, REJECTED o DUPLICATE.
     */
    @POST
    @Path("/facial/{idCliente}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarAccesoFacial(@PathParam("idCliente") int idCliente) {
        try (Connection conn = ConexionDB.getConnection()) {

            // 1. Verificar que el cliente exista, esté activo y revisar su vencimiento
            String sqlCliente =
                    "SELECT nombre, apellido, activo, fecha_vencimiento, " +
                            "(fecha_vencimiento IS NOT NULL AND fecha_vencimiento >= CURRENT_DATE) AS membresia_vigente " +
                            "FROM clientes WHERE id_cliente = ?";

            String nombreCompleto = "";
            boolean activo = false;
            boolean membresiaVigente = false;
            String fechaVencimiento = "N/A";

            try (PreparedStatement psCl = conn.prepareStatement(sqlCliente)) {
                psCl.setInt(1, idCliente);
                try (ResultSet rsCl = psCl.executeQuery()) {
                    if (!rsCl.next()) {
                        return Response.status(404)
                                .entity("{\"estado\": \"REJECTED\", \"mensaje\": \"Cliente no registrado en el sistema\"}")
                                .build();
                    }
                    String nom = rsCl.getString("nombre") != null ? rsCl.getString("nombre").trim() : "";
                    String ape = rsCl.getString("apellido") != null ? rsCl.getString("apellido").trim() : "";
                    nombreCompleto = (nom + " " + ape).trim().replace("\"", "");
                    activo = rsCl.getBoolean("activo");
                    membresiaVigente = rsCl.getBoolean("membresia_vigente");
                    if (rsCl.getDate("fecha_vencimiento") != null) {
                        fechaVencimiento = rsCl.getDate("fecha_vencimiento").toString();
                    }
                }
            }

            if (!activo) {
                return Response.status(403)
                        .entity("{\"estado\": \"REJECTED\", \"cliente\": \"" + nombreCompleto + "\", \"mensaje\": \"Cuenta inactiva\"}")
                        .build();
            }

            if (!membresiaVigente) {
                return Response.status(403)
                        .entity("{\"estado\": \"REJECTED\", \"cliente\": \"" + nombreCompleto + "\", \"fechaVencimiento\": \"" + fechaVencimiento + "\", \"mensaje\": \"Membresía vencida o inexistente\"}")
                        .build();
            }

            // 2. Ventana de duplicados (5 minutos)
            String sqlDup =
                    "SELECT id_asistencia FROM asistencias " +
                            "WHERE id_cliente = ? AND fecha_hora_ingreso >= (CURRENT_TIMESTAMP - INTERVAL '5 minutes') " +
                            "ORDER BY id_asistencia DESC LIMIT 1";

            try (PreparedStatement psDup = conn.prepareStatement(sqlDup)) {
                psDup.setInt(1, idCliente);
                try (ResultSet rsDup = psDup.executeQuery()) {
                    if (rsDup.next()) {
                        return Response.ok(
                                "{\"estado\": \"DUPLICATE\", \"cliente\": \"" + nombreCompleto + "\", \"mensaje\": \"Asistencia ya registrada hace menos de 5 minutos\"}"
                        ).build();
                    }
                }
            }

            // 3. Registrar asistencia válida
            String codigoUnico = "FACIAL_" + idCliente + "_" + System.currentTimeMillis();
            String sqlInsert =
                    "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso, dispositivo_qr, codigo_validado, estado) " +
                            "VALUES (?, CURRENT_TIMESTAMP, 'Reconocimiento Facial', ?, 'ACTIVO')";

            try (PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
                psIns.setInt(1, idCliente);
                psIns.setString(2, codigoUnico);
                psIns.executeUpdate();
            }

            return Response.ok(
                    "{\"estado\": \"ACCEPTED\", \"cliente\": \"" + nombreCompleto + "\", \"fechaVencimiento\": \"" + fechaVencimiento + "\", \"mensaje\": \"¡Bienvenido, " + nombreCompleto + "!\"}"
            ).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity("{\"estado\": \"ERROR\", \"mensaje\": \"Error interno al registrar asistencia\"}")
                    .build();
        }
    }
}
