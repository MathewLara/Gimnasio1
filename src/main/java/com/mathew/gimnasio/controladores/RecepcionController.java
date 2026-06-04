package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RecepcionDAO;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE RECEPCIÓN
 * Orquesta las operaciones del mostrador del gimnasio.
 * Facilita el Punto de Venta (POS), la validación de accesos
 * y las consultas rápidas al directorio de socios.
 */
@Path("/recepcion")
public class RecepcionController {

    // Instancia del objeto de acceso a datos para las consultas exclusivas de recepción
    private RecepcionDAO dao = new RecepcionDAO();

    /**
     * OBTENER TELEMETRÍA DEL DASHBOARD DE RECEPCIÓN
     * @return JSON con KPIs operativos (Aforo actual, caja del día).
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardRecepcion(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\":\"Falta el ID de la empresa.\"}").build();
        }
        // Inyectamos el ID de la empresa al DAO
        String jsonRespuesta = dao.getDashboardRecepJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    /**
     * ENDPOINT PARA REGISTRAR LA ENTRADA/SALIDA FÍSICA AL GIMNASIO
     * Se activa cuando la recepcionista escanea el QR o escribe el ID.
     * @param identificador Código QR escaneado o texto ingresado por teclado.
     */
    @POST
    @Path("/acceso")
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarAcceso(@QueryParam("id") String identificador, @QueryParam("idEmpresa") int idEmpresa) {
        // Validación de seguridad para evitar peticiones nulas o vacías
        if (identificador == null || identificador.trim().isEmpty() || idEmpresa == 0) {
            return Response.ok("{\"status\":\"error\", \"mensaje\":\"Por favor ingrese un código válido y asegure su sucursal.\"}").build();
        }
        // Delega la lógica de Entrada/Salida al DAO filtrando por gimnasio
        String resultado = dao.procesarAccesoQr(identificador, idEmpresa);
        return Response.ok(resultado).build();
    }

    /**
     * ENDPOINT PARA OBTENER EL DIRECTORIO DE SOCIOS
     * Alimenta la barra de búsqueda asíncrona en el modal de pagos del Frontend.
     */
    @GET
    @Path("/socios")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSociosRecepcion(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\":\"Falta el ID de la empresa.\"}").build();
        }
        String jsonRespuesta = dao.obtenerSociosRecepcionJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    /**
     * ENDPOINT: Obtener historial de pagos
     * Recupera la lista de transacciones realizadas para la vista de recepción.
     */
    @GET
    @Path("/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHistorialPagos(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\":\"Falta el ID de la empresa.\"}").build();
        }
        String jsonRespuesta = dao.obtenerHistorialPagosJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    /**
     * ENDPOINT: Registrar nuevo pago desde el modal
     * Procesa el cobro de membresías o planes diarios desde el mostrador.
     * @param payload Objeto JSON dinámico con los detalles financieros.
     */
    @POST
    @Path("/pagos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarNuevoPago(java.util.Map<String, Object> payload) {
        try {
            // Extraemos los datos enviados desde JavaScript parseándolos estrictamente
            int idCliente = Integer.parseInt(payload.get("idCliente").toString());
            int idPlan = Integer.parseInt(payload.get("idPlan").toString());
            double monto = Double.parseDouble(payload.get("monto").toString());
            String metodo = payload.get("metodo").toString();
            // INYECCIÓN: Rescatamos para qué empresa es este dinero
            int idEmpresa = Integer.parseInt(payload.get("idEmpresa").toString());

            // Ejecución de la transacción en la base de datos aislada
            boolean exito = dao.registrarPago(idCliente, idPlan, monto, metodo, idEmpresa);

            // Respuesta HTTP basada en el resultado de la transacción
            if(exito) {
                return Response.ok("{\"status\":\"ok\", \"mensaje\":\"Pago registrado exitosamente.\"}").build();
            } else {
                return Response.status(400).entity("{\"status\":\"error\", \"mensaje\":\"No se pudo registrar el pago en la base de datos.\"}").build();
            }
        } catch(Exception e) {
            // Fallback de seguridad si falta algún dato en el JSON o el parseo numérico falla
            return Response.status(500).entity("{\"status\":\"error\", \"mensaje\":\"Error interno: " + e.getMessage() + "\"}").build();
        }
    }
    @GET
    @Path("/pagos-pendientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagosPendientes(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) return Response.status(400).entity("{\"mensaje\":\"Falta idEmpresa\"}").build();
        String jsonRespuesta = dao.obtenerPagosPendientesJSON(idEmpresa);
        return Response.ok(jsonRespuesta).build();
    }

    @POST
    @Path("/verificar-pago")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarPagoMembresia(java.util.Map<String, Object> payload) {
        try {
            int idPago = Integer.parseInt(payload.get("pagoId").toString());
            String estado = payload.get("estado").toString();
            int idMembresia = Integer.parseInt(payload.get("membresiaId").toString());

            boolean exito = dao.verificarPago(idPago, estado, idMembresia);

            if(exito) {
                return Response.ok("{\"status\":\"ok\"}").build();
            }
            return Response.status(500).entity("{\"status\":\"error\"}").build();
        } catch(Exception e) {
            return Response.status(400).entity("{\"status\":\"error\"}").build();
        }
    }
}