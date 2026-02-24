package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.configuracion.ConexionDB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * CONTROLADOR DE ASISTENCIAS
 * Este controlador gestiona el flujo de entradas y salidas de los clientes al gimnasio.
 * Se activa cuando un cliente escanea su código QR en la recepción.
 */
@Path("/accesos")
public class AsistenciaController {

    /**
     * PROCESAR ACCESO (ENTRADA/SALIDA)
     * Este metodo es el corazón del sistema de recepción.
     * Funciona de forma inteligente: si el cliente no ha entrado hoy, marca ENTRADA.
     * Si ya entró pero no ha salido, marca SALIDA.
     * param idUsuario El ID que viene del código QR (id_usuario de la tabla usuarios)
     * @return Una respuesta JSON con el saludo personalizado y el tipo de movimiento.
     */
    @POST
    @Path("/escanear/{idUsuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarAcceso(@PathParam("idUsuario") int idUsuario) {
        String mensaje = "";
        String tipo = "";

        // Usamos Try-with-resources para asegurar que la conexión se cierre sola al terminar
        try (Connection conn = ConexionDB.getConnection()) {

            /* 1. TRADUCCIÓN DE ID
             * El código QR entrega el 'id_usuario', pero nuestra tabla de asistencias
             * usa el 'id_cliente'. Primero buscamos quién es el cliente dueño de ese usuario.
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
                // Si el ID del QR no existe en la tabla de clientes, detenemos el proceso
                return Response.status(404).entity("{\"mensaje\": \"Usuario no encontrado en clientes\"}").build();
            }

            /* 2. VERIFICAR ESTADO ACTUAL
             * Buscamos si el cliente ya tiene una entrada registrada el día de hoy
             * que todavía no tenga una hora de salida (fecha_hora_salida IS NULL).
             */
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_cliente = ? AND fecha_hora_salida IS NULL AND DATE(fecha_hora_ingreso) = CURRENT_DATE ORDER BY id_asistencia DESC LIMIT 1";
            PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, idCliente);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                /* * CASO A: MARCAR SALIDA
                 * Si encontramos un registro abierto, significa que el cliente está saliendo.
                 * Actualizamos ese registro poniendo la hora actual en 'fecha_hora_salida'.
                 */
                int idAsistencia = rs.getInt("id_asistencia");
                String sqlSalida = "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP WHERE id_asistencia = ?";
                PreparedStatement psUpd = conn.prepareStatement(sqlSalida);
                psUpd.setInt(1, idAsistencia);
                psUpd.executeUpdate();

                mensaje = "👋 ¡Hasta luego, " + nombre + "!";
                tipo = "SALIDA";
            } else {
                /* * CASO B: MARCAR ENTRADA
                 * Si no hay registros abiertos hoy, es un ingreso nuevo.
                 * Insertamos una nueva fila con el ID del cliente y la hora actual.
                 */
                String sqlEntrada = "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso) VALUES (?, CURRENT_TIMESTAMP)";
                PreparedStatement psIns = conn.prepareStatement(sqlEntrada);
                psIns.setInt(1, idCliente);
                psIns.executeUpdate();

                mensaje = "🚀 ¡Bienvenido, " + nombre + "!";
                tipo = "ENTRADA";
            }

            // Devolvemos el resultado al frontend en formato JSON
            return Response.ok("{\"mensaje\": \"" + mensaje + "\", \"tipo\": \"" + tipo + "\"}").build();

        } catch (Exception e) {
            // Si algo falla (ej. conexión a BD), registramos el error en la consola del servidor
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }
}