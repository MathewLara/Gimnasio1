/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ClienteDashboardDAO;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE CLIENTES
 * Centralizo y gestiono todas las interacciones que un cliente tiene en su portal personal.
 * Incluye la recuperación de sus estadísticas (dashboard), marcado de asistencia a rutinas,
 * gestión de su suscripción y el módulo de validación de pagos por comprobante.
 */
@Path("/clientes")
public class ClienteController {

    private ClienteDashboardDAO dao = new ClienteDashboardDAO();

    /**
     * OBTENER DATOS DEL DASHBOARD DEL CLIENTE (GET)
     * Recupero el resumen personalizado del cliente, como días restantes, estado de membresía y progreso.
     * Parametro id: Identificador único del cliente extraído de la URL.
     * Parametro idEmpresa: Identificador de la sucursal a la que pertenece.
     * Retorna: Objeto ResumenClienteDTO con la información consolidada o un error 404 si no existe.
     */
    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dashboard(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // 1. Validación de identificadores para evitar consultas corruptas a la base de datos
        if (id <= 0 || idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Parámetros de acceso inválidos o nulos.\"}").build();
        }

        ResumenClienteDTO datos = dao.obtenerInfoDashboard(id, idEmpresa);
        if (datos != null) return Response.ok(datos).build();

        return Response.status(Response.Status.NOT_FOUND).entity("{\"mensaje\":\"Cliente no encontrado o no pertenece a esta sucursal.\"}").build();
    }

    /**
     * REGISTRAR RUTINA COMPLETADA (POST)
     * Permito al cliente marcar su entrenamiento del día como finalizado, registrando su progreso.
     * Parametro id: Identificador del usuario.
     * Parametro idEmpresa: Identificador de la empresa.
     * Retorna: Confirmación de registro exitoso o aviso si ya se había registrado previamente hoy.
     */
    @POST
    @Path("/{idUsuario}/completar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response completarRutina(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        if (id <= 0 || idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Identificadores de usuario o empresa inválidos.\"}").build();
        }

        boolean exito = dao.registrarTerminoRutina(id, idEmpresa);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Entrenamiento registrado exitosamente.\"}").build();
        } else {
            return Response.ok("{\"mensaje\": \"Ya estabas registrado hoy. ¡Buen trabajo!\"}").build();
        }
    }

    /**
     * CANCELAR SUSCRIPCIÓN (PUT)
     * Proceso la solicitud del cliente para dar de baja su membresía actual en el sistema.
     * Parametro id: Identificador del cliente.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Estado de la operación de cancelación en la base de datos.
     */
    @PUT
    @Path("/{idUsuario}/cancelar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelarSuscripcion(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        if (id <= 0 || idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Datos inválidos para cancelar la suscripción.\"}").build();
        }

        boolean exito = dao.cancelarSuscripcion(id, idEmpresa);
        if (exito) {
            return Response.ok("{\"mensaje\": \"Suscripción cancelada permanentemente.\"}").build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\": \"Error interno al procesar la cancelación.\"}").build();
        }
    }

    /**
     * SUBIR COMPROBANTE DE PAGO DE MEMBRESÍA (POST)
     * Valido y proceso la recepción de un comprobante de transferencia bancaria (en base64)
     * enviado por el cliente, aplicando reglas de negocio estrictas para evitar fraudes o duplicidad.
     * Parametro payload: Mapa JSON que contiene los datos del cliente, membresía, monto y la imagen en base64.
     * Retorna: Confirmación de recepción o mensajes de error detallados si se incumple alguna regla.
     */
    @POST
    @Path("/pago-membresia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subirComprobante(java.util.Map<String, Object> payload) {
        // 1. Validar que la solicitud contenga todos los datos (NullPointerException Safe)
        if (payload == null || !payload.containsKey("id_cliente") || !payload.containsKey("id_membresia") ||
                !payload.containsKey("monto_pagado") || !payload.containsKey("id_empresa") || !payload.containsKey("comprobante")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Faltan datos obligatorios en el formulario de pago.\"}").build();
        }

        try {
            int idCliente = Integer.parseInt(payload.get("id_cliente").toString());
            int idMembresia = Integer.parseInt(payload.get("id_membresia").toString());
            double monto = Double.parseDouble(payload.get("monto_pagado").toString());
            int idEmpresa = Integer.parseInt(payload.get("id_empresa").toString());
            String comprobanteBase64 = payload.get("comprobante").toString().trim();

            // 2. Validaciones numéricas lógicas
            if (idCliente <= 0 || idMembresia <= 0 || idEmpresa <= 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Identificadores de pago inválidos.\"}").build();
            }
            if (monto <= 0) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"El monto de pago no puede ser cero o negativo.\"}").build();
            }

            // 3. Validación de cadena/letras de la imagen: Evita strings vacíos o demasiado cortos para ser una imagen real
            if (comprobanteBase64.isEmpty() || comprobanteBase64.length() < 100) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"El archivo subido no es una imagen válida o está vacío.\"}").build();
            }

            // 4. Reglas de Negocio Estrictas: Integridad relacional y controles Anti-Spam
            if (!dao.existeUsuarioEnEmpresa(idCliente, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\":\"Violación de seguridad: El cliente no pertenece a esta sucursal.\"}").build();
            }
            if (!dao.existeMembresiaEnEmpresa(idMembresia, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\":\"El plan seleccionado no existe o no está disponible en este gimnasio.\"}").build();
            }
            if (dao.tienePagoPendiente(idCliente, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\":\"Ya tienes un comprobante en revisión. Por favor espera a que recepción lo procese antes de enviar otro.\"}").build();
            }

            // 5. Proceder con el registro del pago y almacenamiento del comprobante
            boolean exito = dao.registrarPagoPendiente(idCliente, idMembresia, monto, idEmpresa, comprobanteBase64);

            if(exito) {
                return Response.ok("{\"mensaje\":\"Comprobante recibido y en proceso de revisión.\"}").build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"mensaje\":\"Ocurrió un error en nuestros servidores al guardar la imagen.\"}").build();

        } catch(NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"El formato de los números de pago o los IDs es incorrecto.\"}").build();
        }
    }
}