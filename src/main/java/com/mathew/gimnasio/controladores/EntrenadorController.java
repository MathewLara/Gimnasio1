package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EntrenadorDAO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE ENTRENADORES
 * * Esta clase es el centro de mando para los profesores del gimnasio.
 * Permite que los entrenadores gestionen sus perfiles, creen ejercicios para los alumnos
 * y organicen su agenda de trabajo desde la aplicación.
 */
@Path("/entrenadores")
public class EntrenadorController {

    // El DAO es el asistente que va a la base de datos a traer o guardar la información
    private EntrenadorDAO dao = new EntrenadorDAO();

    /**
     * VER TABLERO PRINCIPAL
     * Carga el resumen del entrenador: cuántos alumnos tiene, sus estadísticas
     * y las rutinas que ha diseñado.
     * URL: GET /api/entrenadores/{id}/dashboard?idEmpresa=X
     */
    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // Le pedimos al asistente (DAO) los datos del profesor logueado filtrando por su empresa
        EntrenadorDashboardDTO dto = dao.obtenerDashboard(id, idEmpresa);
        if (dto != null) {
            return Response.ok(dto).build(); // Si todo sale bien, mandamos los datos en formato JSON
        }
        return Response.status(Response.Status.NOT_FOUND).build(); // Si no existe el profesor, avisamos
    }

    /**
     * CREAR UNA NUEVA RUTINA DESDE LA BIBLIOTECA
     * URL: POST /api/entrenadores/{id}/crearRutina
     */
    @POST
    @Path("/{idUsuario}/crearRutina")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // CORRECCIÓN: Se añadió @QueryParam("idEmpresa") para la arquitectura multi-empresa
    public Response crearRutina(@PathParam("idUsuario") int idUsuario, @QueryParam("idEmpresa") int idEmpresa, NuevaRutinaDTO datos) {
        // CORRECCIÓN: Se inyecta idEmpresa al DAO
        boolean exito = dao.crearRutina(idUsuario, idEmpresa, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina guardada con éxito\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al guardar la rutina\"}").build();
    }

    /**
     * ACTUALIZAR UNA RUTINA EXISTENTE
     * URL: PUT /api/entrenadores/rutinas/{id}
     */
    @PUT
    @Path("/rutinas/{idRutina}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // CORRECCIÓN: Se añadió @QueryParam("idEmpresa")
    public Response modificarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idEmpresa") int idEmpresa, NuevaRutinaDTO datos) {
        // CORRECCIÓN CRÍTICA: Se cambió dao.crearRutina por dao.modificarRutina y se añadió idEmpresa
        boolean exito = dao.modificarRutina(idRutina, idEmpresa, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina editada con éxito\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al editar la rutina\"}").build();
    }

    /**
     * ELIMINAR (DESACTIVAR) UNA RUTINA - LA MANDA A LA PAPELERA
     * URL: DELETE /api/entrenadores/rutinas/{id}
     */
    @DELETE
    @Path("/rutinas/{idRutina}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response borrarRutina(@PathParam("idRutina") int idRutina) {
        boolean exito = dao.desactivarRutina(idRutina);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina movida a la papelera\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al eliminar la rutina\"}").build();
    }

    /**
     * RESTAURAR UNA RUTINA DE LA PAPELERA
     * URL: PUT /api/entrenadores/rutinas/{id}/reactivar
     */
    @PUT
    @Path("/rutinas/{idRutina}/reactivar")
    @Produces(MediaType.APPLICATION_JSON)
    // CORRECCIÓN: Se añadió @QueryParam("idEmpresa")
    public Response restaurarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idEmpresa") int idEmpresa) {
        // CORRECCIÓN: Se inyecta idEmpresa al DAO
        boolean exito = dao.reactivarRutina(idRutina, idEmpresa);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina restaurada con éxito\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al restaurar la rutina\"}").build();
    }

    /**
     * VINCULAR UN NUEVO ALUMNO O ASIGNARLE MÁS RUTINAS
     * URL: POST /api/entrenadores/{id}/alumnos
     */
    @POST
    @Path("/{idUsuario}/alumnos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response vincularAlumno(@PathParam("idUsuario") int idUsuario, com.mathew.gimnasio.modelos.AsignarAlumnoDTO datos) {
        String resultado = dao.vincularAlumno(idUsuario, datos);
        if (resultado.equals("OK")) {
            return Response.ok("{\"mensaje\": \"Rutina asignada exitosamente al alumno.\"}").build();
        } else if (resultado.startsWith("LÍMITE")) {
            // Mandamos Error 400 (Bad Request) para que JavaScript lance la alerta al usuario
            return Response.status(400).entity("{\"mensaje\": \"" + resultado + "\"}").build();
        } else {
            return Response.status(500).entity("{\"mensaje\": \"Error al guardar en base de datos\"}").build();
        }
    }

    /**
     * EDITAR LA RUTINA DE UN ALUMNO (Acumulativo)
     * URL: PUT /api/entrenadores/{id}/alumnos
     */
    @PUT
    @Path("/{idUsuario}/alumnos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarAlumno(@PathParam("idUsuario") int idUsuario, com.mathew.gimnasio.modelos.AsignarAlumnoDTO datos) {
        String resultado = dao.vincularAlumno(idUsuario, datos);
        if (resultado.equals("OK")) {
            return Response.ok("{\"mensaje\": \"Nueva rutina sumada al alumno.\"}").build();
        } else if (resultado.startsWith("LÍMITE")) {
            return Response.status(400).entity("{\"mensaje\": \"" + resultado + "\"}").build();
        } else {
            return Response.status(500).entity("{\"mensaje\": \"Error al guardar en base de datos\"}").build();
        }
    }

    /**
     * DESVINCULAR A UN ALUMNO
     * URL: DELETE /api/entrenadores/{id}/alumnos/{idCliente}
     */
    @DELETE
    @Path("/{idUsuario}/alumnos/{idCliente}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response desvincularAlumno(@PathParam("idUsuario") int idUsuario, @PathParam("idCliente") int idCliente) {
        boolean exito = dao.desvincularAlumno(idUsuario, idCliente);
        if (exito) return Response.ok("{\"mensaje\": \"Alumno desvinculado de tu cartera\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al desvincular al alumno\"}").build();
    }

    /**
     * CONSULTAR AGENDA DEL DÍA (Alumnos citados hoy)
     * URL: GET /api/entrenadores/{id}/agenda?idEmpresa=X
     */
    @GET
    @Path("/{idUsuario}/agenda")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerAgenda(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // Obtenemos la agenda del día filtrando también por el ID de la empresa
        return Response.ok(dao.obtenerAgendaHoy(id, idEmpresa)).build();
    }
}