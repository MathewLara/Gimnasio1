package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ReporteDAO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;

/**
 * Controlador de reportes administrativos (RF08)
 */
@Path("/reportes")
public class ReporteController {

    private ReporteDAO dao = new ReporteDAO();

    private static String hace30Dias() { return LocalDate.now().minusDays(30).toString(); }
    private static String hoy() { return LocalDate.now().toString(); }

    @GET
    @Path("/asistencia")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteAsistencia(
            @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta) {
        String json = dao.reporteAsistencia(
                desde != null ? desde : hace30Dias(),
                hasta != null ? hasta : hoy());
        return Response.ok(json).build();
    }

    @GET
    @Path("/ingresos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteIngresos(
            @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta) {
        String json = dao.reporteIngresos(
                desde != null ? desde : hace30Dias(),
                hasta != null ? hasta : hoy());
        return Response.ok(json).build();
    }

    @GET
    @Path("/rutinas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reporteRutinas() {
        String json = dao.reporteRutinas();
        return Response.ok(json).build();
    }
}
