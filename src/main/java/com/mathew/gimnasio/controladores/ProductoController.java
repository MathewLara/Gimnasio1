package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ProductoDAO;
import com.mathew.gimnasio.modelos.Producto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/productos")
public class ProductoController {

    private ProductoDAO dao = new ProductoDAO();

    // URL: /api/productos
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar() {
        List<Producto> productos = dao.listarProductos();
        return Response.ok(productos).build();
    }

    // URL: /api/productos/{id}/imagen
    @GET
    @Path("/{id}/imagen")
    @Produces("image/jpeg") // Engañamos al navegador diciendo que es JPG (funciona con avif/png también)
    public Response obtenerImagen(@PathParam("id") int id) {
        byte[] imagenBytes = dao.obtenerImagen(id);

        if (imagenBytes != null && imagenBytes.length > 0) {
            return Response.ok(imagenBytes).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}