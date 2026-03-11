package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RecepcionDAO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/recepcion")
public class RecepcionController {

    private RecepcionDAO dao = new RecepcionDAO();

    /**
     * ENDPOINT: DATOS PARA EL DASHBOARD DE RECEPCIÓN
     * Ruta: /api/recepcion/dashboard
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardRecepcion() {
        String jsonRespuesta = dao.getDashboardRecepJSON();
        return Response.ok(jsonRespuesta).build();
    }
}