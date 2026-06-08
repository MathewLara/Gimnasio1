/**
 * Author: Mathew Lara
 * Fecha: 07/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.AdminDAO;
import com.mathew.gimnasio.modelos.DashboardDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE ADMINISTRADOR
 * Gestiona todas las operaciones críticas del panel de administración,
 * incluyendo métricas del dashboard, historial de transacciones,
 * registro manual de pagos y la gestión completa del catálogo de planes.
 */
@Path("/admin")
public class AdminController {

    private AdminDAO adminDAO = new AdminDAO();

    /**
     * OBTENER MÉTRICAS DEL DASHBOARD (GET)
     * Recupera los indicadores clave de rendimiento (KPIs) para la empresa.
     * Parámetro idEmpresa: Identificador de la empresa actual.
     * Retorna: Respuesta HTTP 200 con el objeto DashboardDTO, o HTTP 400 si el ID es inválido.
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID de empresa inválido.\"}").build();
        }
        DashboardDTO stats = adminDAO.obtenerEstadisticas(idEmpresa);
        return Response.ok(stats).build();
    }

    /**
     * OBTENER HISTORIAL DE PAGOS (GET)
     * Recupera el registro histórico de todos los pagos realizados en la sucursal.
     * Parámetro idEmpresa: Identificador numérico de la empresa.
     * Retorna: Respuesta HTTP 200 con un JSON estructurado del historial.
     */
    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagos(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID de empresa inválido.\"}").build();
        }
        return Response.ok(adminDAO.obtenerHistorialPagosJSON(idEmpresa)).build();
    }

    /**
     * REGISTRAR PAGO MANUAL (POST)
     * Procesa y registra un nuevo pago validando exhaustivamente los campos de entrada.
     * Parámetro data: Mapa JSON que contiene idCliente, idPlan, monto, metodo e idEmpresa.
     * Retorna: Mensaje de éxito o error detallado en formato JSON.
     */
    @POST
    @Path("/pagos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postPago(java.util.Map<String, Object> data) {
        // 1. Validar que la petición contenga todos los campos para evitar NullPointerException
        if (data == null || !data.containsKey("idCliente") || !data.containsKey("idPlan") ||
                !data.containsKey("monto") || !data.containsKey("metodo") || !data.containsKey("idEmpresa")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"Faltan datos obligatorios en el formulario de pago.\"}")
                    .build();
        }

        try {
            int idU = Integer.parseInt(data.get("idCliente").toString());
            int idP = Integer.parseInt(data.get("idPlan").toString());
            double m = Double.parseDouble(data.get("monto").toString());
            String met = data.get("metodo").toString().trim();
            int idEmpresa = Integer.parseInt(data.get("idEmpresa").toString());

            // 2. Validaciones de negocio estrictas
            if (idU <= 0 || idP <= 0 || idEmpresa <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\":\"Los identificadores (ID) no pueden ser negativos o cero.\"}")
                        .build();
            }
            if (m <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\":\"El monto de pago no puede ser cero o negativo.\"}")
                        .build();
            }
            if (met.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\":\"Debe seleccionar un método de pago válido.\"}")
                        .build();
            }

            // 3. Procesar pago
            boolean ok = adminDAO.registrarPago(idU, idP, m, met, idEmpresa);
            if (ok) {
                return Response.ok("{\"mensaje\":\"Pago registrado exitosamente.\"}").build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"mensaje\":\"Error al registrar el pago. Verifique si el usuario o el plan existen.\"}").build();
            }

        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"Los formatos de los números enviados son incorrectos.\"}").build();
        }
    }

    // ==========================================
    // ENDPOINTS DE PLANES / MEMBRESÍAS
    // ==========================================

    /**
     * OBTENER CATÁLOGO DE PLANES (GET)
     * Extrae todos los planes de membresía asociados a la empresa para su gestión en el panel.
     * Parámetro idEmpresa: ID de la empresa administradora.
     * Retorna: JSON Array con la lista de planes.
     */
    @GET
    @Path("/planes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlanes(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\": \"ID de empresa inválido.\"}").build();
        }
        return Response.ok(adminDAO.obtenerPlanesJSON(idEmpresa)).build();
    }

    /**
     * CREAR NUEVO PLAN DE MEMBRESÍA (POST)
     * Inserta un nuevo plan en el sistema asegurando que no existan nombres duplicados.
     * Parámetro data: Mapa JSON con idEmpresa, precio, nombre y descripcion.
     * Retorna: Respuesta HTTP indicando éxito o conflicto de datos.
     */
    @POST
    @Path("/planes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearPlan(java.util.Map<String, Object> data) {
        // 1. Evitar NullPointerExceptions
        if (data == null || !data.containsKey("idEmpresa") || !data.containsKey("precio") ||
                !data.containsKey("nombre") || !data.containsKey("descripcion")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"Todos los campos del plan son obligatorios.\"}").build();
        }

        try {
            int idEmpresa = (int) Double.parseDouble(String.valueOf(data.get("idEmpresa")));
            double precio = Double.parseDouble(String.valueOf(data.get("precio")));
            String nombre = String.valueOf(data.get("nombre")).trim();
            String descripcion = String.valueOf(data.get("descripcion")).trim();

            // 2. Validaciones estrictas de datos
            if (idEmpresa <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\":\"ID de empresa no proporcionado o inválido.\"}").build();
            }
            if (nombre.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\":\"El nombre del plan no puede estar vacío.\"}").build();
            }
            if (precio < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"mensaje\":\"El precio del plan no puede ser un valor negativo.\"}").build();
            }

            // 3. Regla de Negocio: No permitir planes con el mismo nombre en la misma empresa
            if (adminDAO.existeNombrePlan(nombre, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"mensaje\":\"Ya existe un plan registrado con este nombre en tu gimnasio.\"}").build();
            }

            // 4. Guardar
            boolean ok = adminDAO.guardarPlan(nombre, precio, descripcion, idEmpresa);
            if(ok) {
                return Response.ok("{\"mensaje\":\"Plan creado exitosamente.\"}").build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"mensaje\":\"Error interno de base de datos al guardar el plan.\"}").build();

        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"Los datos numéricos (precio, ID) tienen un formato incorrecto.\"}").build();
        }
    }

    /**
     * ACTUALIZAR PLAN EXISTENTE (PUT)
     * Edita los datos de un plan, validando previamente que el nombre nuevo no pertenezca a otro plan.
     * Parámetro id: ID en la ruta correspondiente al plan a editar.
     * Parámetro data: JSON con los campos actualizados.
     * Retorna: Estado de la actualización o error en caso de conflicto.
     */
    @PUT
    @Path("/planes/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarPlan(@PathParam("id") int id, java.util.Map<String, Object> data) {
        if (id <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"ID de plan inválido.\"}").build();

        if (data == null || !data.containsKey("idEmpresa") || !data.containsKey("precio") ||
                !data.containsKey("nombre") || !data.containsKey("descripcion")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\":\"Faltan datos obligatorios para la edición.\"}").build();
        }

        try {
            int idEmpresa = (int) Double.parseDouble(String.valueOf(data.get("idEmpresa")));
            double precio = Double.parseDouble(String.valueOf(data.get("precio")));
            String nombre = String.valueOf(data.get("nombre")).trim();
            String descripcion = String.valueOf(data.get("descripcion")).trim();

            if (nombre.isEmpty()) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"El nombre no puede estar vacío.\"}").build();
            if (precio < 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"El precio no puede ser negativo.\"}").build();

            // Verificamos que no esté robando el nombre a otro plan que ya exista
            if (adminDAO.existeNombrePlanEdicion(nombre, idEmpresa, id)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"mensaje\":\"Otro plan diferente ya usa este nombre en tu gimnasio.\"}").build();
            }

            boolean ok = adminDAO.editarPlan(id, nombre, precio, descripcion, idEmpresa);
            if(ok) {
                return Response.ok("{\"mensaje\":\"Plan actualizado exitosamente.\"}").build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\":\"Error al actualizar el plan en la BDD.\"}").build();

        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Error en formato numérico.\"}").build();
        }
    }

    /**
     * CAMBIAR ESTADO DE PLAN (PUT)
     * Habilita o deshabilita un plan en el sistema (soft-delete).
     * Parámetro id: ID del plan a afectar.
     * Parámetro activo: Valor booleano que indica si el plan será visible o no.
     * Retorna: Confirmación del cambio de estado.
     */
    @PUT
    @Path("/planes/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoPlan(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        if (id <= 0) return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"ID inválido.\"}").build();
        adminDAO.cambiarEstadoPlan(id, activo);
        return Response.ok("{\"mensaje\":\"Estado del plan actualizado.\"}").build();
    }

    /**
     * ENDPOINT PÚBLICO: TRAER PLANES ACTIVOS AL INDEX
     * Provee los planes habilitados para ser renderizados dinámicamente en la página principal.
     * Este endpoint no requiere idEmpresa ni token de sesión.
     * Retorna: JSON Array con los planes activos.
     */
    @GET
    @Path("/planes-activos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlanesActivos() {
        String jsonRespuesta = adminDAO.obtenerPlanesActivosJSON();
        return Response.ok(jsonRespuesta).build();
    }
}