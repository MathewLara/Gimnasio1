package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.AdminDAO;
import com.mathew.gimnasio.modelos.DashboardDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE ADMINISTRACIÓN
 * Gestiona los Endpoints exclusivos para el perfil gerencial.
 * Actúa como un proxy entre las peticiones HTTP del DashboardAdmin.js
 * y las sentencias SQL complejas del AdminDAO.
 */
@Path("/admin")
public class AdminController {

    // Instancia del Data Access Object encargado de la persistencia administrativa
    private AdminDAO adminDAO = new AdminDAO();

    /**
     * OBTENER TELEMETRÍA DEL DASHBOARD
     * URL: GET /api/admin/dashboard
     * @return Objeto JSON (DashboardDTO) con los KPIs y métricas consolidadas.
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@QueryParam("idEmpresa") int idEmpresa) { // <- Atrapa el ID de la URL
        if (idEmpresa == 0) return Response.status(400).build();
        DashboardDTO stats = adminDAO.obtenerEstadisticas(idEmpresa);
        return Response.ok(stats).build();
    }

    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagos(@QueryParam("idEmpresa") int idEmpresa) { // <- Atrapa el ID de la URL
        if (idEmpresa == 0) return Response.status(400).build();
        return Response.ok(adminDAO.obtenerHistorialPagosJSON(idEmpresa)).build();
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
        // Atrapamos el ID que nos mande el JS en el JSON
        int idEmpresa = Integer.parseInt(data.get("idEmpresa").toString());

        boolean ok = adminDAO.registrarPago(idU, idP, m, met, idEmpresa);
        return ok ? Response.ok("{\"status\":\"ok\"}").build() : Response.status(400).entity("{\"status\":\"error\"}").build();
    }
}
