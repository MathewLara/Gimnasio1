/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.VentaDAO;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import com.mathew.gimnasio.modelos.PagoMembresiaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE VENTAS
 * Administra el flujo de transacciones comerciales del sistema. 
 * Procesa las solicitudes de compra desde la tienda virtual, verifica la integridad 
 * de los carritos de productos y gestiona el registro persistente de ventas y membresías.
 */
@Path("/ventas")
public class VentaController {

    // Componente de acceso a datos responsable de la persistencia en tablas de ventas y detalles.
    private VentaDAO ventaDAO = new VentaDAO();

    /**
     * PROCESAR UNA VENTA (POST)
     * Procesa y registra una nueva transacción comercial al finalizar una compra en línea.
     * Valida los datos entrantes y consolida la cabecera de la venta junto con sus ítems.
     * Parametro venta: Objeto estructurado que contiene la información completa del carrito.
     * Retorna: Confirmación HTTP indicando si la transacción se guardó correctamente.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarVenta(SolicitudVenta venta) {
        // Traza de auditoría para el registro de entrada de transacciones
        System.out.println("Recibiendo venta por total: " + venta.getTotal());

        // 1. Validación Estructural y de Negocio
        // Verifica que la solicitud contenga un listado válido de productos antes de proceder.
        if (venta.getProductos() == null || venta.getProductos().isEmpty()) {
            return Response.status(400).entity("{\"mensaje\":\"El carrito está vacío\"}").build();
        }

        // 2. Persistencia de Datos
        // Delega la inserción relacional (cabecera y detalles) a la capa de acceso a datos.
        boolean exito = ventaDAO.registrarVenta(venta);

        if (exito) {
            // Retorna confirmación de transacción exitosa al cliente
            return Response.ok("{\"mensaje\":\"Venta procesada correctamente\", \"status\":\"OK\"}").build();
        } else {
            // Manejo de excepciones o fallos en el motor de base de datos
            return Response.status(500).entity("{\"mensaje\":\"Error al guardar la venta en la base de datos\"}").build();
        }
    }

    /**
     * PAGO DE MEMBRESÍA (POST)
     * Registra la recepción de un comprobante de pago asociado a la renovación o 
     * adquisición de una membresía, asignándole un estado de revisión pendiente.
     * Parametro payload: Mapa JSON que incluye información del usuario, la membresía seleccionada, el monto y la evidencia fotográfica.
     * Retorna: Notificación de paso a estado de validación o mensaje de error.
     */
    @POST
    @Path("/membresia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagarMembresia(java.util.Map<String, Object> payload) {
        try {
            int idUsuario = Integer.parseInt(payload.get("idUsuario").toString());
            int idMembresia = Integer.parseInt(payload.get("idMembresia").toString());
            double monto = Double.parseDouble(payload.get("monto").toString());
            int idEmpresa = Integer.parseInt(payload.get("idEmpresa").toString());

            // Extracción de datos complementarios del comprobante, aplicando valores por defecto si aplican.
            String comprobanteFoto = payload.get("comprobanteFoto").toString();
            String numeroReferencia = payload.containsKey("numeroReferencia") ? payload.get("numeroReferencia").toString() : "S/N";
            String motivo = payload.containsKey("motivo") ? payload.get("motivo").toString() : "Renovación";

            if (idUsuario == 0 || monto <= 0) {
                return Response.status(400).entity("{\"mensaje\":\"Datos de pago inválidos\"}").build();
            }

            // Delegación del registro de la transacción a la capa DAO
            String resultado = ventaDAO.registrarPagoMembresia(idUsuario, idMembresia, monto, comprobanteFoto, numeroReferencia, motivo, idEmpresa);

            if (resultado.equals("OK")) {
                return Response.ok("{\"mensaje\":\"¡Pago en revisión! Espera la validación de recepción.\"}").build();
            } else {
                return Response.status(500).entity("{\"mensaje\":\"" + resultado + "\"}").build();
            }
        } catch(Exception e) {
            return Response.status(400).entity("{\"mensaje\":\"Faltan datos o la foto en el formulario\"}").build();
        }
    }

    /**
     * OBTENER VENTAS PENDIENTES (GET)
     * Recupera el listado de transacciones o pedidos comerciales que requieren 
     * entrega física o atención por parte del personal administrativo.
     * Parametro idEmpresa: Identificador numérico de la empresa a consultar.
     * Retorna: Lista en formato JSON de objetos VentaPendienteDTO.
     */
    @GET
    @Path("/pendientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVentasPendientes(@QueryParam("idEmpresa") int idEmpresa) {
        // Validación de credenciales o identificadores requeridos
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\":\"Falta la empresa\"}").build();
        }

        // Extracción de la lista de transacciones inconclusas
        java.util.List<com.mathew.gimnasio.modelos.VentaPendienteDTO> pendientes = ventaDAO.obtenerVentasPendientes(idEmpresa);
        return Response.ok(pendientes).build();
    }

    /**
     * MARCAR PRODUCTO COMO ENTREGADO (PUT)
     * Actualiza el estado logístico de una factura específica indicando que 
     * los productos correspondientes han sido despachados al cliente final.
     * Parametro idFactura: Identificador único de la transacción a actualizar.
     * Retorna: Confirmación de actualización del estado de entrega.
     */
    @PUT
    @Path("/{id}/entregar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response entregarProducto(@PathParam("id") int idFactura) {
        boolean exito = ventaDAO.marcarComoEntregado(idFactura);

        if (exito) {
            return Response.ok("{\"mensaje\":\"Producto marcado como entregado\"}").build();
        } else {
            return Response.status(500).entity("{\"mensaje\":\"Error al actualizar el estado de la entrega\"}").build();
        }
    }

    /**
     * OBTENER DETALLES DE FACTURA (GET)
     * Extrae el desglose completo de los ítems asociados a una transacción 
     * específica, ideal para visualización de recibos o impresión física.
     * Parametro idFactura: Identificador único de la factura.
     * Retorna: Lista de mapas JSON con el detalle de artículos de la venta.
     */
    @GET
    @Path("/{id}/detalles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetallesFactura(@PathParam("id") int idFactura) {
        java.util.List<java.util.Map<String, Object>> detalles = ventaDAO.obtenerDetallesFactura(idFactura);
        return Response.ok(detalles).build();
    }
}