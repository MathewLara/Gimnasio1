package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ProductoDAO;
import com.mathew.gimnasio.modelos.Producto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * CONTROLADOR DE PRODUCTOS
 * * Este componente es el encargado de gestionar el catálogo de la tienda del gimnasio.
 * Permite que la página web obtenga la lista de suplementos, accesorios y, lo más importante,
 * las fotografías de los productos que están guardadas en la base de datos.
 */
@Path("/productos")
public class ProductoController {

    // El DAO es nuestro "almacenero", es quien sabe ir a las tablas de SQL a buscar los productos
    private ProductoDAO dao = new ProductoDAO();

    /**
     * LISTAR PRODUCTOS
     * * Se usa para cargar toda la vitrina de la tienda. Devuelve nombres, descripciones y precios.
     * URL: GET /api/productos
     * * @return Una lista completa de productos en formato JSON para que la web los muestre.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar() {
        // Pedimos al almacenero (DAO) que nos traiga lo que hay en estanteria (Base de Datos)
        List<Producto> productos = dao.listarProductos();
        // Entregamos la lista con un código 200 (OK)
        return Response.ok(productos).build();
    }

    /**
     * OBTENER IMAGEN DEL PRODUCTO
     * * Este metodo es especial: no devuelve texto, devuelve los "bytes" de la imagen.
     * Gracias a esto, las etiquetas <img> de HTML pueden mostrar las fotos guardadas en SQL.
     * URL: GET /api/productos/{id}/imagen
     * * @param id El número identificador del producto cuya foto queremos ver.
     */
    @GET
    @Path("/{id}/imagen")
    @Produces("image/jpeg") // Engañamos al navegador diciendo que es JPG (funciona con avif/png también)
    public Response obtenerImagen(@PathParam("id") int id) {
        // El DAO busca en la columna de tipo 'bytea' (binario) de la base de datos
        byte[] imagenBytes = dao.obtenerImagen(id);

        // Si el producto tiene una foto cargada...
        if (imagenBytes != null && imagenBytes.length > 0) {
            // Enviamos los datos de la imagen directamente para que se renderice en pantalla
            return Response.ok(imagenBytes).build();
        } else {
            // Si no hay foto, respondemos que no se encontró nada (404)
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}