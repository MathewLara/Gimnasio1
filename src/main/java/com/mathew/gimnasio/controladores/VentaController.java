package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.VentaDAO;
import com.mathew.gimnasio.modelos.SolicitudVenta;
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
            return Response.ok("{\"mensaje\":\"Venta procesada correctamente\", \"status\":\"OK\"}").build();
        } else {
            // Si hubo un error (ej. se cayó la base de datos), enviamos un error 500
            return Response.status(500).entity("{\"mensaje\":\"Error al guardar la venta en la base de datos\"}").build();
        }
    }

    /**
     * GET /ventas/{id}/comprobante - Obtener comprobante de venta (RF07)
     */
    @GET
    @Path("/{id}/comprobante")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComprobante(@PathParam("id") int id) {
        String json = ventaDAO.obtenerComprobanteJSON(id);
        if (json != null) return Response.ok(json).build();
        return Response.status(404).entity("{\"mensaje\":\"Comprobante no encontrado\"}").build();
    }
}
