package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.MembresiaDAO;
import com.mathew.gimnasio.modelos.Membresia;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/membresias")
public class MembresiaController {

    private MembresiaDAO membresiaDAO = new MembresiaDAO();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerMembresias(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa.\"}").build();
        }

        List<Membresia> membresias = membresiaDAO.listarPorEmpresa(idEmpresa);
        return Response.ok(membresias).build();
    }
}