package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.AdminDAO;
import com.mathew.gimnasio.modelos.DashboardDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin")
public class AdminController {

    private AdminDAO adminDAO = new AdminDAO();

    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard() {
        DashboardDTO stats = adminDAO.obtenerEstadisticas();
        return Response.ok(stats).build();
    }
}
