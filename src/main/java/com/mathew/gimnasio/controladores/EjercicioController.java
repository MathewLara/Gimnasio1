package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EjercicioDAO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Controlador de ejercicios (RF05) - Catálogo
 */
@Path("/rutinas")
public class EjercicioController {

    private EjercicioDAO dao = new EjercicioDAO();

    @GET
    @Path("/ejercicios")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCatalogoEjercicios() {
        String json = dao.listarEjerciciosJSON();
        return Response.ok(json).build();
    }
}
