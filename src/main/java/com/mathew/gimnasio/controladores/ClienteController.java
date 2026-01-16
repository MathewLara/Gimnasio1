package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ClienteDashboardDAO;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/clientes")
public class ClienteController {
    private ClienteDashboardDAO dao = new ClienteDashboardDAO();

    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dashboard(@PathParam("idUsuario") int id) {
        ResumenClienteDTO datos = dao.obtenerInfoDashboard(id);
        if (datos != null) return Response.ok(datos).build();
        return Response.status(404).entity("{\"mensaje\":\"Cliente no encontrado\"}").build();
    }
}