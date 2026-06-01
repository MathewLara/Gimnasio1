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
    public Response crearRutina(@PathParam("idUsuario") int idUsuario, @QueryParam("idEmpresa") int idEmpresa, NuevaRutinaDTO datos) {
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
    public Response modificarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idEmpresa") int idEmpresa, NuevaRutinaDTO datos) {
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
    public Response restaurarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idEmpresa") int idEmpresa) {
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
        // CORRECCIÓN: Ahora atrapa el boolean correctamente
        boolean exito = dao.vincularAlumno(idUsuario, datos);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Rutina asignada exitosamente al alumno.\"}").build();
        } else {
            // Si devuelve false, asumimos que es por la regla de negocio del límite de 10
            return Response.status(400).entity("{\"mensaje\": \"Límite de rutinas alcanzado (Máx 10). Ocurrió un error en la asignación.\"}").build();
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
        // CORRECCIÓN: Ahora atrapa el boolean correctamente
        boolean exito = dao.vincularAlumno(idUsuario, datos);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Nueva rutina sumada al alumno.\"}").build();
        } else {
            return Response.status(400).entity("{\"mensaje\": \"No se pudo sumar la rutina. Es posible que el alumno ya tenga el límite de 10 activas.\"}").build();
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
        try {
            String nombre = String.valueOf(data.get("nombre"));
            String grupo = String.valueOf(data.get("grupo"));

            boolean ok = dao.guardarEjercicio(nombre, grupo);
            if(ok) return Response.ok("{\"mensaje\":\"Ejercicio creado\"}").build();
            return Response.status(500).entity("{\"mensaje\":\"Error al crear en base de datos\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(400).entity("{\"mensaje\":\"Error de formato: " + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/ejercicios/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarEjercicio(@PathParam("id") int id, java.util.Map<String, Object> data) {
        try {
            String nombre = String.valueOf(data.get("nombre"));
            String grupo = String.valueOf(data.get("grupo"));

            boolean ok = dao.editarEjercicio(id, nombre, grupo);
            if(ok) return Response.ok("{\"mensaje\":\"Ejercicio actualizado\"}").build();
            return Response.status(500).entity("{\"mensaje\":\"Error al actualizar en base de datos\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(400).entity("{\"mensaje\":\"Error de formato: " + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/ejercicios/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoEjercicio(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        dao.cambiarEstadoEjercicio(id, activo);
        return Response.ok("{\"mensaje\":\"Estado del ejercicio actualizado\"}").build();
    }
}