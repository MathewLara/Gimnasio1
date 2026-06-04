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
    public Response getDashboard(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) return Response.status(400).build();
        DashboardDTO stats = adminDAO.obtenerEstadisticas(idEmpresa);
        return Response.ok(stats).build();
    }

    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagos(@QueryParam("idEmpresa") int idEmpresa) {
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
        int idEmpresa = Integer.parseInt(data.get("idEmpresa").toString());

        boolean ok = adminDAO.registrarPago(idU, idP, m, met, idEmpresa);
        return ok ? Response.ok("{\"status\":\"ok\"}").build() : Response.status(400).entity("{\"status\":\"error\"}").build();
    }

    // ==========================================
    // ENDPOINTS DE PLANES / MEMBRESÍAS
    // ==========================================
    @GET
    @Path("/planes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlanes(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) return Response.status(400).build();
        return Response.ok(adminDAO.obtenerPlanesJSON(idEmpresa)).build();
    }

    @POST
    @Path("/planes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearPlan(java.util.Map<String, Object> data) {
        try {
            // Solución blindada para números flotantes provenientes de JS
            int idEmpresa = (int) Double.parseDouble(String.valueOf(data.get("idEmpresa")));
            double precio = Double.parseDouble(String.valueOf(data.get("precio")));

            String nombre = String.valueOf(data.get("nombre"));
            String descripcion = String.valueOf(data.get("descripcion"));

            boolean ok = adminDAO.guardarPlan(nombre, precio, descripcion, idEmpresa);
            if(ok) return Response.ok("{\"mensaje\":\"Plan creado\"}").build();
            return Response.status(500).entity("{\"mensaje\":\"Error al crear en la BDD\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(400).entity("{\"mensaje\":\"Error en Java: " + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/planes/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarPlan(@PathParam("id") int id, java.util.Map<String, Object> data) {
        try {
            // Solución blindada para números flotantes provenientes de JS
            int idEmpresa = (int) Double.parseDouble(String.valueOf(data.get("idEmpresa")));
            double precio = Double.parseDouble(String.valueOf(data.get("precio")));

            String nombre = String.valueOf(data.get("nombre"));
            String descripcion = String.valueOf(data.get("descripcion"));

            boolean ok = adminDAO.editarPlan(id, nombre, precio, descripcion, idEmpresa);
            if(ok) return Response.ok("{\"mensaje\":\"Plan actualizado\"}").build();
            return Response.status(500).entity("{\"mensaje\":\"Error al actualizar en la BDD\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(400).entity("{\"mensaje\":\"Error en Java: " + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/planes/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoPlan(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        adminDAO.cambiarEstadoPlan(id, activo);
        return Response.ok("{\"mensaje\":\"Estado actualizado\"}").build();
    }
    /**
     * ENDPOINT PÚBLICO: TRAER PLANES ACTIVOS AL INDEX
     * Este endpoint no requiere idEmpresa ni token de sesión
     */
    @GET
    @Path("/planes-activos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlanesActivos() {
        // Llamamos al método que acabamos de crear en el DAO
        String jsonRespuesta = adminDAO.obtenerPlanesActivosJSON();
        return Response.ok(jsonRespuesta).build();
    }

}