/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RecepcionDAO;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE RECEPCIÓN
 * Gestiona las operaciones diarias del personal de mostrador,
 * incluyendo el control de acceso de los socios mediante códigos QR,
 * el registro rápido de pagos presenciales y la verificación de comprobantes.
 */
@Path("/recepcion")
public class RecepcionController {

    private RecepcionDAO dao = new RecepcionDAO();

    /**
     * OBTENER DASHBOARD DE RECEPCIÓN (GET)
     * Recupera las métricas rápidas necesarias para la pantalla principal del mostrador,
     * como accesos del día o alertas de pagos pendientes.
     * Parametro idEmpresa: Identificador numérico de la sucursal.
     * Retorna: JSON estructurado con la información del dashboard o error si el ID es inválido.
     */
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

    /**
     * REGISTRAR ACCESO DE SOCIOS (POST)
     * Procesa la lectura de un código QR para validar si un cliente tiene acceso
     * permitido a las instalaciones en ese momento.
     * Parametro identificador: Código alfanumérico leído del QR del socio.
     * Parametro idEmpresa: Identificador de la sucursal donde se intenta acceder.
     * Retorna: Confirmación de acceso concedido o mensaje detallado de denegación (ej. falta de pago).
     */
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

    /**
     * LISTAR SOCIOS DE LA SUCURSAL (GET)
     * Provee al personal de recepción un listado completo de los clientes activos
     * pertenecientes exclusivamente a su sucursal.
     * Parametro idEmpresa: Identificador de la empresa.
     * Retorna: JSON Array con la información de los socios.
     */
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

    /**
     * OBTENER HISTORIAL DE PAGOS (GET)
     * Recupera el registro de las transacciones financieras procesadas en el mostrador
     * para facilitar el cierre de caja.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: Colección JSON con el historial de pagos.
     */
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

    /**
     * REGISTRAR NUEVO PAGO EN MOSTRADOR (POST)
     * Valida y procesa un pago físico o transferencia recibida directamente por el personal,
     * asegurando que los montos y métodos sean válidos antes de activar la membresía.
     * Parametro payload: Mapa JSON que contiene idCliente, idPlan, monto, metodo e idEmpresa.
     * Retorna: Estado de la operación de cobro y activación de membresía.
     */
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

    /**
     * OBTENER PAGOS PENDIENTES DE VERIFICACIÓN (GET)
     * Recupera la lista de comprobantes que los clientes han subido desde su portal
     * para que recepción los revise y apruebe.
     * Parametro idEmpresa: Identificador de la sucursal.
     * Retorna: JSON Array con los pagos en estado pendiente.
     */
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

    /**
     * VERIFICAR COMPROBANTE DE PAGO (POST)
     * Procesa la decisión del personal de recepción sobre un comprobante pendiente
     * (Aprobado o Rechazado), aplicando la lógica de activación correspondiente.
     * Parametro payload: Datos de la transacción incluyendo el ID del pago, el nuevo estado y el ID de membresía.
     * Retorna: Confirmación de la actualización del estado del pago.
     */
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