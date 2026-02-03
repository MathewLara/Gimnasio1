package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EntrenadorDAO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/entrenadores")
public class EntrenadorController {

    private EntrenadorDAO dao = new EntrenadorDAO();

    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dashboard(@PathParam("idUsuario") int id) {
        EntrenadorDashboardDTO datos = dao.obtenerDashboard(id);

        if (datos != null) {
            return Response.ok(datos).build();
        }
        return Response.status(404).entity("{\"mensaje\":\"Entrenador no encontrado\"}").build();
    }
    @POST
    @Path("/{idUsuario}/crearRutina")
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearRutina(@PathParam("idUsuario") int id, com.mathew.gimnasio.modelos.NuevaRutinaDTO datos) {
        boolean exito = dao.crearRutina(id, datos);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Rutina creada con éxito\"}").build();
        }
        return Response.status(500).entity("{\"mensaje\": \"Error al crear rutina\"}").build();
    }
}