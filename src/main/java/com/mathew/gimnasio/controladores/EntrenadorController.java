/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.EntrenadorDAO;
import com.mathew.gimnasio.modelos.EntrenadorDashboardDTO;
import com.mathew.gimnasio.modelos.NuevaRutinaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE ENTRENADORES
 * Gestiona las operaciones del módulo de entrenadores, incluyendo la visualización del dashboard,
 * la creación y edición de rutinas de ejercicios, la vinculación con alumnos
 * y la administración del catálogo de ejercicios.
 */
@Path("/entrenadores")
public class EntrenadorController {

    private EntrenadorDAO dao = new EntrenadorDAO();

    /**
     * OBTENER MÉTRICAS DEL DASHBOARD DEL ENTRENADOR (GET)
     * Recupera el resumen estadístico y las métricas clave para el panel principal del entrenador.
     * Parametro idUsuario: Identificador único del entrenador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Objeto JSON con la información del dashboard o error 404 si no se encuentra.
     */
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

    /**
     * CREAR NUEVA RUTINA DE ENTRENAMIENTO (POST)
     * Valida y almacena una nueva rutina en la biblioteca personal del entrenador,
     * asegurando que no existan duplicados por nombre.
     * Parametro idUsuario: Identificador del entrenador creador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro datos: Objeto que contiene el nombre de la rutina y la lista de IDs de ejercicios.
     * Retorna: Confirmación de guardado exitoso o mensaje de error detallado en caso de conflicto.
     */
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

    /**
     * MODIFICAR RUTINA EXISTENTE (PUT)
     * Edita los detalles de una rutina previamente guardada, verificando que
     * el nuevo nombre no genere conflictos en la biblioteca del entrenador.
     * Parametro idRutina: Identificador de la rutina a modificar.
     * Parametro idUsuario: Identificador del entrenador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Parametro datos: Objeto con los datos actualizados de la rutina.
     * Retorna: Estado de la actualización en la base de datos.
     */
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

    /**
     * ELIMINAR RUTINA (DELETE)
     * Desactiva una rutina específica (borrado lógico), moviéndola a la papelera
     * en lugar de eliminarla físicamente para mantener el historial.
     * Parametro idRutina: Identificador de la rutina a desactivar.
     * Retorna: Confirmación de la operación de borrado lógico.
     */
    @DELETE
    @Path("/rutinas/{idRutina}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response borrarRutina(@PathParam("idRutina") int idRutina) {
        if (idRutina <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID inválido.\"}").build();
        boolean exito = dao.desactivarRutina(idRutina);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina movida a la papelera.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al eliminar la rutina.\"}").build();
    }

    /**
     * RESTAURAR RUTINA ELIMINADA (PUT)
     * Reactiva una rutina que había sido enviada a la papelera, volviéndola a hacer visible.
     * Parametro idRutina: Identificador de la rutina.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Confirmación de la restauración exitosa.
     */
    @PUT
    @Path("/rutinas/{idRutina}/reactivar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restaurarRutina(@PathParam("idRutina") int idRutina, @QueryParam("idEmpresa") int idEmpresa) {
        if (idRutina <= 0 || idEmpresa <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"Datos de reactivación incompletos.\"}").build();
        boolean exito = dao.reactivarRutina(idRutina, idEmpresa);
        if (exito) return Response.ok("{\"mensaje\": \"Rutina restaurada con éxito.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al restaurar la rutina.\"}").build();
    }

    /**
     * VINCULAR ALUMNO A RUTINA (POST)
     * Asigna un plan de entrenamiento a un alumno específico, verificando previamente
     * que no se excedan los límites máximos permitidos en la base de datos.
     * Parametro idUsuario: Identificador del entrenador.
     * Parametro datos: Objeto con la información de la vinculación y el alumno.
     * Retorna: Mensaje de éxito o advertencia si se alcanzó el límite de asignaciones.
     */
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

    /**
     * ACTUALIZAR ASIGNACIÓN DE ALUMNO (PUT)
     * Añade una nueva rutina a la lista activa de un alumno, asegurando
     * las restricciones de cantidad máxima establecidas por negocio.
     * Parametro idUsuario: Identificador del entrenador.
     * Parametro datos: Datos actualizados de la vinculación a procesar.
     * Retorna: Confirmación o mensaje de error por límite superado.
     */
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

    /**
     * DESVINCULAR ALUMNO (DELETE)
     * Retira a un alumno de la cartera activa del entrenador de manera segura.
     * Parametro idUsuario: Identificador del entrenador.
     * Parametro idCliente: Identificador del alumno a desvincular.
     * Retorna: Estado de la operación de desvinculación.
     */
    @DELETE
    @Path("/{idUsuario}/alumnos/{idCliente}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response desvincularAlumno(@PathParam("idUsuario") int idUsuario, @PathParam("idCliente") int idCliente) {
        if (idCliente <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID de alumno inválido.\"}").build();
        boolean exito = dao.desvincularAlumno(idUsuario, idCliente);
        if (exito) return Response.ok("{\"mensaje\": \"Alumno desvinculado de tu cartera.\"}").build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error al desvincular al alumno.\"}").build();
    }

    /**
     * OBTENER AGENDA DEL DÍA (GET)
     * Recupera la lista de actividades o alumnos programados para el día actual del entrenador.
     * Parametro id: Identificador del entrenador.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Lista JSON con los registros de la agenda para el día en curso.
     */
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

    /**
     * OBTENER CATÁLOGO DE EJERCICIOS (GET)
     * Recupera la lista completa de ejercicios disponibles en el sistema para armar rutinas.
     * Retorna: JSON Array con todos los ejercicios registrados.
     */
    @GET
    @Path("/ejercicios")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEjercicios() {
        return Response.ok(dao.obtenerEjerciciosJSON()).build();
    }

    /**
     * CREAR NUEVO EJERCICIO (POST)
     * Añade un nuevo ejercicio al catálogo general, validando que el nombre no exista previamente
     * para evitar redundancias en la base de datos.
     * Parametro data: Mapa JSON con el nombre y grupo muscular del ejercicio.
     * Retorna: Confirmación de creación exitosa o mensaje de conflicto por duplicidad.
     */
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

    /**
     * EDITAR EJERCICIO EXISTENTE (PUT)
     * Modifica los datos de un ejercicio registrado, asegurando la integridad
     * de los nombres únicos en el catálogo durante la actualización.
     * Parametro id: Identificador del ejercicio a modificar.
     * Parametro data: Mapa JSON con el nombre y grupo actualizados.
     * Retorna: Respuesta indicando el resultado de la operación en base de datos.
     */
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

    /**
     * CAMBIAR ESTADO DE EJERCICIO (PUT)
     * Habilita o deshabilita un ejercicio específico mediante borrado lógico,
     * permitiendo ocultarlo sin afectar historiales de rutinas pasadas.
     * Parametro id: Identificador del ejercicio.
     * Parametro activo: Booleano con el nuevo estado a aplicar.
     * Retorna: Confirmación del cambio de estado.
     */
    @PUT
    @Path("/ejercicios/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoEjercicio(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        if (id <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID inválido.\"}").build();
        dao.cambiarEstadoEjercicio(id, activo);
        return Response.ok("{\"mensaje\":\"Estado del ejercicio actualizado\"}").build();
    }
}