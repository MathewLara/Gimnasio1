package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EntrenadorDAO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/entrenadores")
public class EntrenadorController {

    private EntrenadorDAO dao = new EntrenadorDAO();

    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        if (id <= 0 || idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Parámetros de acceso inválidos.\"}").build();
        }
        EntrenadorDashboardDTO dto = dao.obtenerDashboard(id, idEmpresa);
        if (dto != null) {
            return Response.ok(dto).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/{idUsuario}/crearRutina")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearRutina(@PathParam("idUsuario") int idUsuario, @QueryParam("idEmpresa") int idEmpresa, NuevaRutinaDTO datos) {
        // 1. Validaciones estructurales para evitar NullPointerException
        if (datos == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"La solicitud está vacía.\"}").build();
        }
        if (datos.getNombreRutina() == null || datos.getNombreRutina().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"El nombre de la rutina es obligatorio.\"}").build();
        }
        if (datos.getIdsEjercicios() == null || datos.getIdsEjercicios().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Debe seleccionar al menos un ejercicio para la rutina.\"}").build();
        }

        // 2. Regla de Negocio: No duplicar nombres de rutinas en la biblioteca del mismo entrenador
        if (dao.existeNombreRutina(datos.getNombreRutina(), idUsuario, idEmpresa, 0)) {
            return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\": \"Ya tienes una rutina guardada con este mismo nombre.\"}").build();
        }

        boolean exito = dao.crearRutina(idUsuario, idEmpresa, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina guardada con éxito.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error interno al guardar la rutina.\"}").build();
    }

    @PUT
    @Path("/rutinas/{idRutina}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modificarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idUsuario") int idUsuario, @QueryParam("idEmpresa") int idEmpresa, NuevaRutinaDTO datos) {
        if (idRutina <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID de rutina inválido.\"}").build();
        }
        if (datos == null || datos.getNombreRutina() == null || datos.getNombreRutina().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"El nombre de la rutina no puede estar vacío.\"}").build();
        }
        if (datos.getIdsEjercicios() == null || datos.getIdsEjercicios().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"La rutina no puede quedarse sin ejercicios.\"}").build();
        }

        // Validar duplicidad excluyendo la rutina actual
        if (dao.existeNombreRutina(datos.getNombreRutina(), idUsuario, idEmpresa, idRutina)) {
            return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\": \"Otro plan ya usa este nombre en tu biblioteca.\"}").build();
        }

        boolean exito = dao.modificarRutina(idRutina, idEmpresa, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina editada con éxito.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al editar la rutina en la base de datos.\"}").build();
    }

    @DELETE
    @Path("/rutinas/{idRutina}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response borrarRutina(@PathParam("idRutina") int idRutina) {
        if (idRutina <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID inválido.\"}").build();
        boolean exito = dao.desactivarRutina(idRutina);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina movida a la papelera.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al eliminar la rutina.\"}").build();
    }

    @PUT
    @Path("/rutinas/{idRutina}/reactivar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restaurarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idEmpresa") int idEmpresa) {
        if (idRutina <= 0 || idEmpresa <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Datos de reactivación incompletos.\"}").build();
        boolean exito = dao.reactivarRutina(idRutina, idEmpresa);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina restaurada con éxito.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al restaurar la rutina.\"}").build();
    }

    @POST
    @Path("/{idUsuario}/alumnos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response vincularAlumno(@PathParam("idUsuario") int idUsuario, com.mathew.gimnasio.modelos.AsignarAlumnoDTO datos) {
        if (datos == null || datos.getIdCliente() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Debe seleccionar un alumno válido.\"}").build();
        }

        boolean exito = dao.vincularAlumno(idUsuario, datos);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Rutina asignada exitosamente al alumno.\"}").build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\": \"Límite de rutinas alcanzado (Máx 10) u ocurrió un error de asignación.\"}").build();
        }
    }

    @PUT
    @Path("/{idUsuario}/alumnos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarAlumno(@PathParam("idUsuario") int idUsuario, com.mathew.gimnasio.modelos.AsignarAlumnoDTO datos) {
        if (datos == null || datos.getIdCliente() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Debe enviar los datos del alumno.\"}").build();
        }
        boolean exito = dao.vincularAlumno(idUsuario, datos);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Nueva rutina sumada al alumno.\"}").build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\": \"No se pudo sumar la rutina. Límite de 10 activas alcanzado.\"}").build();
        }
    }

    @DELETE
    @Path("/{idUsuario}/alumnos/{idCliente}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response desvincularAlumno(@PathParam("idUsuario") int idUsuario, @PathParam("idCliente") int idCliente) {
        if (idCliente <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID de alumno inválido.\"}").build();
        boolean exito = dao.desvincularAlumno(idUsuario, idCliente);
        if (exito) return Response.ok("{\"mensaje\": \"Alumno desvinculado de tu cartera.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al desvincular al alumno.\"}").build();
    }

    @GET
    @Path("/{idUsuario}/agenda")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerAgenda(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        if (id <= 0 || idEmpresa <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Faltan credenciales.\"}").build();
        return Response.ok(dao.obtenerAgendaHoy(id, idEmpresa)).build();
    }

    // ==========================================
    // ENDPOINTS DE EJERCICIOS
    // ==========================================
    @GET
    @Path("/ejercicios")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEjercicios() {
        return Response.ok(dao.obtenerEjerciciosJSON()).build();
    }

    @POST
    @Path("/ejercicios")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearEjercicio(java.util.Map<String, Object> data) {
        if (data == null || !data.containsKey("nombre") || !data.containsKey("grupo")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Faltan datos obligatorios (nombre, grupo).\"}").build();
        }

        String nombre = String.valueOf(data.get("nombre")).trim();
        String grupo = String.valueOf(data.get("grupo")).trim();

        if (nombre.isEmpty() || grupo.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"El nombre y el grupo muscular no pueden estar vacíos.\"}").build();
        }

        // Validación contra duplicados para mantener el catálogo limpio
        if (dao.existeNombreEjercicio(nombre, 0)) {
            return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\": \"Ya existe un ejercicio registrado con ese nombre exacto.\"}").build();
        }

        boolean ok = dao.guardarEjercicio(nombre, grupo);
        if(ok) return Response.status(Response.Status.CREATED).entity("{\"mensaje\":\"Ejercicio creado\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\":\"Error al crear en base de datos\"}").build();
    }

    @PUT
    @Path("/ejercicios/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarEjercicio(@PathParam("id") int id, java.util.Map<String, Object> data) {
        if (id <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID inválido.\"}").build();
        if (data == null || !data.containsKey("nombre") || !data.containsKey("grupo")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Faltan datos obligatorios para editar.\"}").build();
        }

        String nombre = String.valueOf(data.get("nombre")).trim();
        String grupo = String.valueOf(data.get("grupo")).trim();

        if (nombre.isEmpty() || grupo.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"El nombre no puede estar vacío.\"}").build();
        }

        if (dao.existeNombreEjercicio(nombre, id)) {
            return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\": \"Otro ejercicio ya utiliza este nombre en el catálogo.\"}").build();
        }

        boolean ok = dao.editarEjercicio(id, nombre, grupo);
        if(ok) return Response.ok("{\"mensaje\":\"Ejercicio actualizado\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\":\"Error al actualizar en base de datos\"}").build();
    }

    @PUT
    @Path("/ejercicios/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoEjercicio(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        if (id <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID inválido.\"}").build();
        dao.cambiarEstadoEjercicio(id, activo);
        return Response.ok("{\"mensaje\":\"Estado del ejercicio actualizado\"}").build();
    }
}