package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RecepcionDAO;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/recepcion")
public class RecepcionController {

    private RecepcionDAO dao = new RecepcionDAO();

    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardRecepcion(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"ID de empresa inválido.\"}").build();
        }
        String jsonRespuesta = dao.getDashboardRecepJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    @POST
    @Path("/acceso")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarAcceso(@QueryParam("id") String identificador, @QueryParam("idEmpresa") int idEmpresa) {
        if (identificador == null || identificador.trim().isEmpty() || idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"status\":\"error\", \"mensaje\":\"Por favor ingrese un código QR válido y verifique su sucursal.\"}")
                    .build();
        }
        String resultado = dao.procesarAccesoQr(identificador, idEmpresa);
        return Response.ok(resultado).build();
    }

    @GET
    @Path("/socios")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSociosRecepcion(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"ID de empresa inválido.\"}").build();
        }
        String jsonRespuesta = dao.obtenerSociosRecepcionJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHistorialPagos(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"ID de empresa inválido.\"}").build();
        }
        String jsonRespuesta = dao.obtenerHistorialPagosJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    @POST
    @Path("/pagos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarNuevoPago(java.util.Map<String, Object> payload) {
        // 1. Validar que la petición contenga todos los campos obligatorios
        if (payload == null || !payload.containsKey("idCliente") || !payload.containsKey("idPlan") ||
                !payload.containsKey("monto") || !payload.containsKey("metodo") || !payload.containsKey("idEmpresa")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"status\":\"error\", \"mensaje\":\"Faltan datos obligatorios para procesar el pago.\"}")
                    .build();
        }

        try {
            int idUsuario = Integer.parseInt(payload.get("idCliente").toString());
            int idPlan = Integer.parseInt(payload.get("idPlan").toString());
            double monto = Double.parseDouble(payload.get("monto").toString());
            String metodo = payload.get("metodo").toString().trim();
            int idEmpresa = Integer.parseInt(payload.get("idEmpresa").toString());

            // 2. Validaciones estrictas de formatos y números lógicos
            if (idUsuario <= 0 || idPlan <= 0 || idEmpresa <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\":\"error\", \"mensaje\":\"Los identificadores deben ser válidos y mayores a cero.\"}")
                        .build();
            }
            if (monto <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\":\"error\", \"mensaje\":\"No se pueden registrar pagos en cero o con valores negativos.\"}")
                        .build();
            }

            // 3. Limitar métodos de pago permitidos en mostrador
            if (!metodo.equalsIgnoreCase("Efectivo") &&
                    !metodo.equalsIgnoreCase("Tarjeta") &&
                    !metodo.equalsIgnoreCase("Transferencia")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\":\"error\", \"mensaje\":\"Método de pago no reconocido. Use: Efectivo, Tarjeta o Transferencia.\"}")
                        .build();
            }

            // 4. Validar integridad referencial cruzada (Seguridad Multi-Tenant)
            if (!dao.existeUsuarioEnEmpresa(idUsuario, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"status\":\"error\", \"mensaje\":\"El usuario seleccionado no pertenece a este gimnasio.\"}")
                        .build();
            }
            if (!dao.existePlanEnEmpresa(idPlan, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"status\":\"error\", \"mensaje\":\"El plan seleccionado no es válido para este gimnasio.\"}")
                        .build();
            }

            // 5. Ejecución
            boolean exito = dao.registrarPago(idUsuario, idPlan, monto, metodo, idEmpresa);

            if(exito) {
                return Response.ok("{\"status\":\"ok\", \"mensaje\":\"Pago registrado y membresía activada exitosamente.\"}").build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"status\":\"error\", \"mensaje\":\"Error interno de base de datos al guardar el cobro.\"}")
                        .build();
            }
        } catch(NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"status\":\"error\", \"mensaje\":\"Los formatos de los valores numéricos son incorrectos.\"}")
                    .build();
        }
    }

    @GET
    @Path("/pagos-pendientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagosPendientes(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"mensaje\":\"ID de empresa inválido.\"}").build();
        }
        String jsonRespuesta = dao.obtenerPagosPendientesJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    @POST
    @Path("/verificar-pago")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarPagoMembresia(java.util.Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("pagoId") || !payload.containsKey("estado") || !payload.containsKey("membresiaId") || !payload.containsKey("idEmpresa")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"status\":\"error\", \"mensaje\":\"Faltan datos (pagoId, estado, membresiaId, idEmpresa).\"}")
                    .build();
        }

        try {
            int idPago = Integer.parseInt(payload.get("pagoId").toString());
            String estado = payload.get("estado").toString().trim().toUpperCase();
            int idMembresia = Integer.parseInt(payload.get("membresiaId").toString());
            int idEmpresa = Integer.parseInt(payload.get("idEmpresa").toString());

            // 1. Validaciones básicas
            if (idPago <= 0 || idMembresia <= 0 || idEmpresa <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\":\"error\", \"mensaje\":\"Los IDs enviados son inválidos.\"}")
                        .build();
            }
            if (!estado.equals("APROBADO") && !estado.equals("RECHAZADO")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\":\"error\", \"mensaje\":\"El estado solo puede ser APROBADO o RECHAZADO.\"}")
                        .build();
            }

            // 2. Seguridad: Evitar doble verificación o modificar pagos ajenos
            if (!dao.esPagoPendienteValido(idPago, idEmpresa)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"status\":\"error\", \"mensaje\":\"El pago no existe, pertenece a otra sucursal, o ya fue procesado previamente.\"}")
                        .build();
            }

            // 3. Ejecución
            boolean exito = dao.verificarPago(idPago, estado, idMembresia);

            if(exito) {
                return Response.ok("{\"status\":\"ok\"}").build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"status\":\"error\", \"mensaje\":\"Error en la base de datos al procesar el comprobante.\"}")
                    .build();

        } catch(NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"status\":\"error\", \"mensaje\":\"Formato numérico incorrecto en los IDs.\"}")
                    .build();
        }
    }
}