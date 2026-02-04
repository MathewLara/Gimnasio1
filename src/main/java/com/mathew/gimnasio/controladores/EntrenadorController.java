package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EntrenadorDAO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/entrenadores")
public class EntrenadorController {

    private EntrenadorDAO dao = new EntrenadorDAO();

    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@PathParam("idUsuario") int id) {
        EntrenadorDashboardDTO dto = dao.obtenerDashboard(id);
        if (dto != null) return Response.ok(dto).build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/{idUsuario}/crearRutina")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearRutina(@PathParam("idUsuario") int id, NuevaRutinaDTO datos) {
        boolean exito = dao.crearRutina(id, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina creada\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error\"}").build();
    }

    @GET
    @Path("/{idUsuario}/agenda")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerAgenda(@PathParam("idUsuario") int id) {
        return Response.ok(dao.obtenerAgendaHoy(id)).build();
    }

    // --- NUEVO: ENDPOINT PARA DESACTIVAR ---
    @DELETE
    @Path("/rutinas/{idRutina}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarRutina(@PathParam("idRutina") int idRutina) {
        boolean exito = dao.desactivarRutina(idRutina);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Rutina desactivada\"}").build();
        }
        return Response.status(500).entity("{\"mensaje\": \"No se pudo eliminar\"}").build();
    }
    // --- EDITAR RUTINA ---
    @PUT
    @Path("/rutinas/{idRutina}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarRutina(@PathParam("idRutina") int id, NuevaRutinaDTO datos) {
        boolean exito = dao.actualizarRutina(id, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina actualizada\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al editar\"}").build();
    }

    // --- REACTIVAR RUTINA ---
    @PUT
    @Path("/rutinas/{idRutina}/reactivar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivarRutina(@PathParam("idRutina") int id) {
        boolean exito = dao.reactivarRutina(id);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina restaurada\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al restaurar\"}").build();
    }
}