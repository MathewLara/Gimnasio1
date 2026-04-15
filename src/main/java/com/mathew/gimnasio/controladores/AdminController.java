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
    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagos() {
        return Response.ok(adminDAO.obtenerHistorialPagosJSON()).build();
    }

    @POST
    @Path("/pagos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postPago(java.util.Map<String, Object> data) {
        int idU = Integer.parseInt(data.get("idCliente").toString());
        int idP = Integer.parseInt(data.get("idPlan").toString());
        double m = Double.parseDouble(data.get("monto").toString());
        String met = data.get("metodo").toString();

        boolean ok = adminDAO.registrarPago(idU, idP, m, met);
        return ok ? Response.ok("{\"status\":\"ok\"}").build()
                : Response.status(400).entity("{\"status\":\"error\"}").build();
    }
}
