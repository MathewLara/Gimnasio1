package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.VentaDAO;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import com.mathew.gimnasio.modelos.PagoMembresiaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE VENTAS
 * * Este componente es el "Cajero" de nuestro gimnasio. Su función principal es recibir
 * las solicitudes de compra de la tienda online, verificar que el carrito sea válido
 * y dar la orden para que la venta se guarde de forma permanente en los registros.
 */
@Path("/ventas")
public class VentaController {

    // El VentaDAO es el encargado de escribir en las tablas de 'ventas' y 'detalles_ventas'
    private VentaDAO ventaDAO = new VentaDAO();

    /**
     * PROCESAR UNA VENTA
     * * Este metodo se activa cuando el cliente presiona el botón "Finalizar Compra" en la tienda.
     * Recibe un objeto con el ID del cliente, el total de dinero y la lista de productos.
     * * URL: POST /api/ventas
     * @param venta Objeto que contiene toda la información del carrito de compras.
     * @return Respuesta confirmando si la compra se realizó o si hubo un problema.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON) // Recibe la lista de compras en formato JSON
    @Produces(MediaType.APPLICATION_JSON) // Responde con un mensaje de confirmación en JSON
    public Response procesarVenta(SolicitudVenta venta) {
        // Imprimimos en la consola del servidor para que el programador vea que llegó la petición
        System.out.println("Recibiendo venta por total: " + venta.getTotal());

        /** VALIDACIÓN DE SEGURIDAD:
         * Antes de intentar guardar, verificamos que el carrito no llegue vacío.
         * Si no hay productos, respondemos con un error 400 (Petición incorrecta).
         */
        if (venta.getProductos() == null || venta.getProductos().isEmpty()) {
            return Response.status(400).entity("{\"mensaje\":\"El carrito está vacío\"}").build();
        }

        /* * LLAMADA AL DAO:
         * Le pedimos al VentaDAO que guarde la información en PostgreSQL.
         * El DAO se encargará de insertar la cabecera de la venta y cada uno de sus productos.
         */
        boolean exito = ventaDAO.registrarVenta(venta);

        if (exito) {
            // Si el DAO nos confirma que se guardó bien, enviamos el mensaje de éxito
            return Response.ok("{\"mensaje\":\"Venta procesada correctamente\", \"status\":\"OK\"}").build();
        } else {
            // Si hubo un error (ej. se cayó la base de datos), enviamos un error 500
            return Response.status(500).entity("{\"mensaje\":\"Error al guardar la venta en la base de datos\"}").build();
        }
    }
    /**
     * ENDPOINT: PAGO DE MEMBRESÍA
     */
    @POST
    @Path("/membresia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagarMembresia(PagoMembresiaDTO req) {
        if (req.getIdUsuario() == 0 || req.getMonto() <= 0) {
            return Response.status(400).entity("{\"mensaje\":\"Datos de pago inválidos\"}").build();
        }

        // AQUÍ ESTÁ LA MAGIA: Cambiamos 'boolean exito' por 'String resultado'
        String resultado = ventaDAO.registrarPagoMembresia(req.getIdUsuario(), req.getIdMembresia(), req.getMonto(), req.getDias());

        if (resultado.equals("OK")) {
            return Response.ok("{\"mensaje\":\"¡Pago exitoso y membresía renovada!\"}").build();
        } else {
            // Mandamos el error de la base de datos directo a la web
            return Response.status(500).entity("{\"mensaje\":\"" + resultado + "\"}").build();
        }
    }
    /**
     * ENDPOINT: Obtener pedidos pendientes para el Dashboard del Admin
     * GET /api/ventas/pendientes
     */
    @GET
    @Path("/pendientes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVentasPendientes(@QueryParam("idEmpresa") int idEmpresa) {
        // Validación de seguridad
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\":\"Falta la empresa\"}").build();
        }

        // Le pasamos el ID de la empresa al DAO
        java.util.List<com.mathew.gimnasio.modelos.VentaPendienteDTO> pendientes = ventaDAO.obtenerVentasPendientes(idEmpresa);
        return Response.ok(pendientes).build();
    }
    /**
     * ENDPOINT: Marcar un producto como entregado
     * PUT /api/ventas/{id}/entregar
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
     * ENDPOINT: Obtener detalles de una factura para imprimir
     * GET /api/ventas/{id}/detalles
     */
    @GET
    @Path("/{id}/detalles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetallesFactura(@PathParam("id") int idFactura) {
        java.util.List<java.util.Map<String, Object>> detalles = ventaDAO.obtenerDetallesFactura(idFactura);
        return Response.ok(detalles).build();
    }
}
