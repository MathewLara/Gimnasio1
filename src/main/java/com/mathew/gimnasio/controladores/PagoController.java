package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.PagoDAO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Controlador de pagos (RF07)
 */
@Path("/pagos")
public class PagoController {

    private PagoDAO dao = new PagoDAO();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagos(@QueryParam("idCliente") Integer idCliente, @QueryParam("limite") Integer limite) {
        String json = dao.obtenerPagosJSON(idCliente, limite != null ? limite : 100);
        return Response.ok(json).build();
    }
}
