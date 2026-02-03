package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ClienteDashboardDAO;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.POST;

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
    @POST
    @Path("/{idUsuario}/completar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response completarRutina(@PathParam("idUsuario") int id) {
        // Llamamos al DAO para guardar en la base de datos
        boolean exito = dao.registrarTerminoRutina(id);

        if (exito) {
            return Response.ok("{\"mensaje\": \"Entrenamiento registrado exitosamente\"}").build();
        } else {
            // Si devuelve false, asumimos que ya estaba registrado hoy, pero respondemos OK
            // para que el cliente vea el botón verde y no se preocupe.
            return Response.ok("{\"mensaje\": \"Ya estaba registrado hoy\"}").build();
        }
    }
}