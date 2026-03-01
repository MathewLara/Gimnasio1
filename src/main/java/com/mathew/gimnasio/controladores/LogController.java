package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.LogAccesoDAO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Controlador de logs de acceso (RF09)
 */
@Path("/logs")
public class LogController {

    private LogAccesoDAO dao = new LogAccesoDAO();

    @GET
    @Path("/accesos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogsAccesos(@QueryParam("limite") Integer limite) {
        String json = dao.obtenerLogsJSON(limite != null ? limite : 100);
        return Response.ok(json).build();
    }
}
