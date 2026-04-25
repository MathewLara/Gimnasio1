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
    public Response getDashboard() {
        DashboardDTO stats = adminDAO.obtenerEstadisticas();
        return Response.ok(stats).build(); // HTTP 200 OK
    }

    /**
     * OBTENER HISTORIAL DE CAJA (PAGOS)
     * URL: GET /api/admin/pagos
     * @return String JSON nativo con el historial financiero.
     */
    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagos() {
        return Response.ok(adminDAO.obtenerHistorialPagosJSON()).build();
    }

    /**
     * REGISTRAR NUEVO PAGO (PUNTO DE VENTA)
     * Procesa una transacción financiera de forma segura.
     * URL: POST /api/admin/pagos
     * @param data Payload JSON dinámico parseado como un Map de Java.
     */
    @POST
    @Path("/pagos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postPago(java.util.Map<String, Object> data) {
        // 1. Extracción y parseo estricto de los datos enviados desde el frontend
        int idU = Integer.parseInt(data.get("idCliente").toString());
        int idP = Integer.parseInt(data.get("idPlan").toString());
        double m = Double.parseDouble(data.get("monto").toString());
        String met = data.get("metodo").toString();

        // 2. Delegación de la transacción al DAO
        boolean ok = adminDAO.registrarPago(idU, idP, m, met);

        // 3. Respuesta HTTP condicional basada en el éxito de la transacción SQL
        return ok ? Response.ok("{\"status\":\"ok\"}").build()
                : Response.status(400).entity("{\"status\":\"error\"}").build(); // HTTP 400 Bad Request
    }
}
