package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.configuracion.ConexionDB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Path("/accesos")
public class AsistenciaController {

    // AHORA RECIBIMOS EL ID DE USUARIO (El que sale en el QR/Dashboard)
    @POST
    @Path("/escanear/{idUsuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarAcceso(@PathParam("idUsuario") int idUsuario) {
        String mensaje = "";
        String tipo = "";

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. TRADUCCIÓN: BUSCAR EL ID_CLIENTE USANDO EL ID_USUARIO
            // (Esto arregla el problema de "Sin Registro")
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
                return Response.status(404).entity("{\"mensaje\": \"Usuario no encontrado en clientes\"}").build();
            }

            // 2. AHORA SÍ, VERIFICAMOS ENTRADA/SALIDA CON EL ID CORRECTO (idCliente)
            String sqlCheck = "SELECT id_asistencia FROM asistencias WHERE id_cliente = ? AND fecha_hora_salida IS NULL AND DATE(fecha_hora_ingreso) = CURRENT_DATE ORDER BY id_asistencia DESC LIMIT 1";
            PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setInt(1, idCliente);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                // --- MARCAR SALIDA ---
                int idAsistencia = rs.getInt("id_asistencia");
                String sqlSalida = "UPDATE asistencias SET fecha_hora_salida = CURRENT_TIMESTAMP WHERE id_asistencia = ?";
                PreparedStatement psUpd = conn.prepareStatement(sqlSalida);
                psUpd.setInt(1, idAsistencia);
                psUpd.executeUpdate();

                mensaje = "👋 ¡Hasta luego, " + nombre + "!";
                tipo = "SALIDA";
            } else {
                // --- MARCAR ENTRADA ---
                String sqlEntrada = "INSERT INTO asistencias (id_cliente, fecha_hora_ingreso) VALUES (?, CURRENT_TIMESTAMP)";
                PreparedStatement psIns = conn.prepareStatement(sqlEntrada);
                psIns.setInt(1, idCliente);
                psIns.executeUpdate();

                mensaje = "🚀 ¡Bienvenido, " + nombre + "!";
                tipo = "ENTRADA";
            }

            return Response.ok("{\"mensaje\": \"" + mensaje + "\", \"tipo\": \"" + tipo + "\"}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"mensaje\": \"Error interno\"}").build();
        }
    }
}