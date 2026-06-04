package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ClienteDashboardDAO;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/clientes")
public class ClienteController {

    private ClienteDashboardDAO dao = new ClienteDashboardDAO();

    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dashboard(@PathParam("idUsuario") int id, @QueryParam("idEmpresa") int idEmpresa) {
        // 1. Validación de identificadores
        if (id <= 0 || idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"Parámetros de acceso inválidos o nulos.\"}").build();
        }

        ResumenClienteDTO datos = dao.obtenerInfoDashboard(id, idEmpresa);
        if (datos != null) return Response.ok(datos).build();

        return Response.status(Response.Status.NOT_FOUND).entity("{\"mensaje\":\"Cliente no encontrado o no pertenece a esta sucursal.\"}").build();
    }

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

            // 4. Reglas de Negocio Estrictas: Integridad y Anti-Spam
            if (!dao.existeUsuarioEnEmpresa(idCliente, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\":\"Violación de seguridad: El cliente no pertenece a esta sucursal.\"}").build();
            }
            if (!dao.existeMembresiaEnEmpresa(idMembresia, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\":\"El plan seleccionado no existe o no está disponible en este gimnasio.\"}").build();
            }
            if (dao.tienePagoPendiente(idCliente, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT).entity("{\"mensaje\":\"Ya tienes un comprobante en revisión. Por favor espera a que recepción lo procese antes de enviar otro.\"}").build();
            }

            // 5. Proceder con el guardado en base de datos
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