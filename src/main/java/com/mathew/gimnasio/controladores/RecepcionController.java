package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RecepcionDAO;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/recepcion")
public class RecepcionController {

    private RecepcionDAO dao = new RecepcionDAO();

    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardRecepcion() {
        String jsonRespuesta = dao.getDashboardRecepJSON();
        return Response.ok(jsonRespuesta).build();
    }

    /**
     * ENDPOINT PARA REGISTRAR LA ENTRADA/SALIDA FÍSICA AL GIMNASIO
     * Se activa cuando la recepcionista escanea el QR o escribe el ID.
     */
    @POST
    @Path("/acceso")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarAcceso(@QueryParam("id") String identificador) {
        if (identificador == null || identificador.trim().isEmpty()) {
            return Response.ok("{\"status\":\"error\", \"mensaje\":\"Por favor ingrese un código o usuario.\"}").build();
        }
        String resultado = dao.procesarAccesoQr(identificador);
        return Response.ok(resultado).build();
    }
}