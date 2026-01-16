package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.VentaDAO;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/ventas")
public class VentaController {

    private VentaDAO ventaDAO = new VentaDAO();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarVenta(SolicitudVenta venta) {
        System.out.println("Recibiendo venta por total: " + venta.getTotal());

        if (venta.getProductos() == null || venta.getProductos().isEmpty()) {
            return Response.status(400).entity("{\"mensaje\":\"El carrito está vacío\"}").build();
        }

        boolean exito = ventaDAO.registrarVenta(venta);

        if (exito) {
            return Response.ok("{\"mensaje\":\"Venta procesada correctamente\", \"status\":\"OK\"}").build();
        } else {
            return Response.status(500).entity("{\"mensaje\":\"Error al guardar la venta en la base de datos\"}").build();
        }
    }
}
