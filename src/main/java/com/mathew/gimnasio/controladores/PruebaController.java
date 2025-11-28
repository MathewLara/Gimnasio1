package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RolDAO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/prueba")
public class PruebaController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response test() {
        RolDAO dao = new RolDAO();
        return Response.ok(dao.obtenerRoles()).build();
    }
}