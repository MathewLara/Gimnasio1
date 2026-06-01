package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.SuperAdminDAO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/superadmin")
public class SuperAdminController {

    private SuperAdminDAO dao = new SuperAdminDAO();

    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard() {
        return Response.ok(dao.getDashboardJSON()).build();
    }

    @GET
    @Path("/empresas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmpresas() {
        return Response.ok(dao.getEmpresasJSON()).build();
    }

    @POST
    @Path("/empresas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearEmpresa(Map<String, String> data) {
        boolean ok = dao.guardarEmpresa(data.get("nombre"), data.get("ruc"), data.get("telefono"), data.get("direccion"));
        if(ok) return Response.ok("{\"mensaje\":\"Empresa creada\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error (RUC duplicado)\"}").build();
    }

    @PUT
    @Path("/empresas/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarEmpresa(@PathParam("id") int id, Map<String, String> data) {
        boolean ok = dao.editarEmpresa(id, data.get("nombre"), data.get("ruc"), data.get("telefono"), data.get("direccion"));
        if(ok) return Response.ok("{\"mensaje\":\"Empresa actualizada\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error al actualizar\"}").build();
    }

    @PUT
    @Path("/empresas/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoEmpresa(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        dao.cambiarEstadoEmpresa(id, activo);
        return Response.ok("{\"mensaje\":\"Estado actualizado\"}").build();
    }

    @GET
    @Path("/administradores")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdministradores() {
        return Response.ok(dao.getAdministradoresJSON()).build();
    }

    @POST
    @Path("/administradores")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearAdmin(Map<String, String> data) {
        int idEmpresa = Integer.parseInt(String.valueOf(data.get("idEmpresa")));
        boolean ok = dao.guardarAdmin(idEmpresa, data.get("nombre"), data.get("apellido"), data.get("usuario"), data.get("contrasena"));
        if(ok) return Response.ok("{\"mensaje\":\"Dueño creado\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error (Usuario duplicado)\"}").build();
    }

    @PUT
    @Path("/administradores/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarAdmin(@PathParam("id") int id, Map<String, String> data) {
        int idEmpresa = Integer.parseInt(String.valueOf(data.get("idEmpresa")));
        boolean ok = dao.editarAdmin(id, idEmpresa, data.get("nombre"), data.get("apellido"), data.get("usuario"), data.get("contrasena"));
        if(ok) return Response.ok("{\"mensaje\":\"Dueño actualizado\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error al actualizar\"}").build();
    }

    @PUT
    @Path("/administradores/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoAdmin(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        dao.cambiarEstadoAdmin(id, activo);
        return Response.ok("{\"mensaje\":\"Estado actualizado\"}").build();
    }
}