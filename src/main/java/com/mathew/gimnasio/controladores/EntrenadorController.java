package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EntrenadorDAO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.EntrenadorDTO;
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
     * URL: GET /api/entrenadores/{id}/dashboard
     */
    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@PathParam("idUsuario") int id) {
        // Le pedimos al asistente (DAO) los datos del profesor Mike o el que esté logueado
        EntrenadorDashboardDTO dto = dao.obtenerDashboard(id);
        if (dto != null) return Response.ok(dto).build(); // Todo salió bien, entregamos los datos
        return Response.status(Response.Status.NOT_FOUND).build(); // No encontramos al entrenador
    }

    /**
     * CREAR NUEVA RUTINA
     * Se usa cuando el entrenador termina de armar un plan de ejercicios para un alumno
     * y presiona el botón "Guardar".
     * URL: POST /api/entrenadores/{id}/crearRutina
     */
    @POST
    @Path("/{idUsuario}/crearRutina")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearRutina(@PathParam("idUsuario") int id, NuevaRutinaDTO datos) {
        // Intentamos guardar los ejercicios recibidos en la base de datos
        boolean exito = dao.crearRutina(id, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina creada\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error\"}").build();
    }

    /**
     * CONSULTAR AGENDA
     * Muestra la lista de actividades o alumnos que el entrenador tiene programados para hoy.
     * URL: GET /api/entrenadores/{id}/agenda
     */
    @GET
    @Path("/{idUsuario}/agenda")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerAgenda(@PathParam("idUsuario") int id) {
        // Traemos la lista de compromisos del día desde la base de datos
        return Response.ok(dao.obtenerAgendaHoy(id)).build();
    }

    /**
     * ELIMINAR O DESACTIVAR RUTINA
     * Si una rutina ya no se usa o fue un error, el entrenador puede borrarla de la vista.
     * URL: DELETE /api/entrenadores/rutinas/{idRutina}
     */
    @DELETE
    @Path("/rutinas/{idRutina}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarRutina(@PathParam("idRutina") int idRutina) {
        // Le pedimos al sistema que desactive la rutina para que no aparezca más
        boolean exito = dao.desactivarRutina(idRutina);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Rutina desactivada\"}").build();
        }
        return Response.status(500).entity("{\"mensaje\": \"No se pudo eliminar\"}").build();
    }
    /**
     * MODIFICAR RUTINA EXISTENTE
     * Permite al entrenador cambiar ejercicios, series o repeticiones de una rutina ya creada.
     * URL: PUT /api/entrenadores/rutinas/{idRutina}
     */
    @PUT
    @Path("/rutinas/{idRutina}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarRutina(@PathParam("idRutina") int id, NuevaRutinaDTO datos) {
        // Actualizamos los datos con la nueva información enviada
        boolean exito = dao.actualizarRutina(id, datos);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina actualizada\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al editar\"}").build();
    }

    /**
     * RESTAURAR RUTINA
     * Si el entrenador eliminó una rutina por error, este botón permite traerla de vuelta.
     * URL: PUT /api/entrenadores/rutinas/{idRutina}/reactivar
     */
    @PUT
    @Path("/rutinas/{idRutina}/reactivar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivarRutina(@PathParam("idRutina") int id) {
        boolean exito = dao.reactivarRutina(id);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina restaurada\"}").build();
        return Response.status(500).entity("{\"mensaje\": \"Error al restaurar\"}").build();
    }

    // ========== CRUD ENTRENADORES (RF04) ==========

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarEntrenadores() {
        return Response.ok(dao.listarEntrenadoresJSON()).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerEntrenador(@PathParam("id") int id) {
        String json = dao.obtenerEntrenadorJSON(id);
        if (json != null) return Response.ok(json).build();
        return Response.status(404).entity("{\"mensaje\":\"Entrenador no encontrado\"}").build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearEntrenador(EntrenadorDTO dto) {
        if (dao.crearEntrenador(dto)) return Response.ok("{\"mensaje\":\"Entrenador creado\"}").build();
        return Response.status(400).entity("{\"mensaje\":\"Error al crear entrenador\"}").build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarEntrenador(@PathParam("id") int id, EntrenadorDTO dto) {
        if (dao.actualizarEntrenador(id, dto)) return Response.ok("{\"mensaje\":\"Entrenador actualizado\"}").build();
        return Response.status(400).entity("{\"mensaje\":\"Error al actualizar\"}").build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarEntrenador(@PathParam("id") int id) {
        if (dao.eliminarEntrenador(id)) return Response.ok("{\"mensaje\":\"Entrenador desactivado\"}").build();
        return Response.status(400).entity("{\"mensaje\":\"Error al eliminar\"}").build();
    }
}