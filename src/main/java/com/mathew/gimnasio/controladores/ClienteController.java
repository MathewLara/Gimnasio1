package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ClienteDashboardDAO;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;

/**
 * CONTROLADOR DE CLIENTES
 * * Clase encargada de exponer los recursos REST para la entidad Cliente.
 * Actúa como la capa de comunicación entre las peticiones HTTP del frontend
 * y la lógica de acceso a datos del backend.
 */
@Path("/clientes")
public class ClienteController {
    private ClienteDashboardDAO dao = new ClienteDashboardDAO();

    /**
     * Recupera el conjunto de datos necesarios para renderizar el Dashboard del cliente.
     * * El recurso es accesible mediante el metodo HTTP GET. Retorna un objeto JSON
     * que contiene el perfil, asistencias y ejercicios actuales del usuario.
     * * URL: GET /api/clientes/{idUsuario}/dashboard?idEmpresa=X
     * * @param id Identificador de usuario extraído de la ruta de la URL.
     * @return Response Objeto de respuesta HTTP con el DTO serializado en formato JSON.
     */
    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    // SE AÑADIÓ idEmpresa
    public Response dashboard(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // Delegación de la lógica de negocio al componente DAO especializado (pasando idEmpresa)
        ResumenClienteDTO datos = dao.obtenerInfoDashboard(id, idEmpresa);
        // Respuesta exitosa (HTTP 200) con el cuerpo del mensaje poblado
        if (datos != null) return Response.ok(datos).build();
        // Respuesta de error controlado (HTTP 404) cuando el recurso no existe
        return Response.status(404).entity("{\"mensaje\":\"Cliente no encontrado\"}").build();
    }

    /**
     * Procesa la notificación de finalización de una sesión de entrenamiento.
     * * Este recurso utiliza el metodo HTTP POST para realizar una escritura persistente
     * en el historial de actividades del gimnasio.
     * * URL: POST /api/clientes/{idUsuario}/completar?idEmpresa=X
     * * @param id Identificador del usuario que finaliza la actividad.
     * @return Response Confirmación de la operación o aviso de registro existente.
     */
    @POST
    @Path("/{idUsuario}/completar")
    @Produces(MediaType.APPLICATION_JSON)
    // SE AÑADIÓ idEmpresa
    public Response completarRutina(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // Ejecución de la persistencia de datos mediante el DAO (pasando idEmpresa)
        boolean exito = dao.registrarTerminoRutina(id, idEmpresa);

        if (exito) {
            // Confirmación de inserción correcta en la base de datos
            return Response.ok("{\"mensaje\": \"Entrenamiento registrado exitosamente\"}").build();
        } else {
            // Si devuelve false, asumimos que ya estaba registrado hoy, pero respondemos OK
            // para que el cliente vea el botón verde y no se preocupe.
            return Response.ok("{\"mensaje\": \"Ya estaba registrado hoy\"}").build();
        }
    }

    /**
     * Procesa la cancelación de la suscripción del cliente.
     * URL: PUT /api/clientes/{idUsuario}/cancelar?idEmpresa=X
     */
    @PUT
    @Path("/{idUsuario}/cancelar")
    @Produces(MediaType.APPLICATION_JSON)
    // SE AÑADIÓ idEmpresa
    public Response cancelarSuscripcion(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // Pasando idEmpresa por seguridad
        boolean exito = dao.cancelarSuscripcion(id, idEmpresa);

        if (exito) {
            return Response.ok("{\"mensaje\": \"Suscripción cancelada permanentemente\"}").build();
        } else {
            return Response.status(500).entity("{\"mensaje\": \"Error al cancelar la suscripción\"}").build();
        }
    }
    /**
     * Endpoint para recibir la foto del comprobante en Base64
     */
    @POST
    @Path("/pago-membresia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subirComprobante(java.util.Map<String, Object> payload) {
        try {
            int idCliente = Integer.parseInt(payload.get("id_cliente").toString());
            int idMembresia = Integer.parseInt(payload.get("id_membresia").toString());
            double monto = Double.parseDouble(payload.get("monto_pagado").toString());
            int idEmpresa = Integer.parseInt(payload.get("id_empresa").toString());
            String comprobanteBase64 = payload.get("comprobante").toString();

            boolean exito = dao.registrarPagoPendiente(idCliente, idMembresia, monto, idEmpresa, comprobanteBase64);

            if(exito) {
                return Response.ok("{\"status\":\"ok\", \"mensaje\":\"Comprobante recibido\"}").build();
            }
            return Response.status(500).entity("{\"status\":\"error\", \"mensaje\":\"Error al guardar el comprobante\"}").build();
        } catch(Exception e) {
            return Response.status(400).entity("{\"status\":\"error\", \"mensaje\":\"Datos inválidos\"}").build();
        }
    }
}