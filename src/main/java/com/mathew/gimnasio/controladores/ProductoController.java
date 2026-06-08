/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ProductoDAO;
import com.mathew.gimnasio.modelos.Producto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * CONTROLADOR DE PRODUCTOS
 * Gestiona el catálogo de la tienda del gimnasio.
 * Permite la obtención de la lista de suplementos, accesorios y
 * la recuperación de las fotografías de los productos almacenadas en la base de datos.
 */
@Path("/productos")
public class ProductoController {

    // Instancia de ProductoDAO encargada del acceso a datos para la entidad Producto.
    private ProductoDAO dao = new ProductoDAO();

    /**
     * LISTAR PRODUCTOS (GET)
     * Recupera el listado completo de productos disponibles para la vitrina de la tienda,
     * incluyendo nombres, descripciones y precios, filtrados por la empresa correspondiente.
     * Parametro idEmpresa: Identificador numérico de la empresa a consultar.
     * Retorna: Una lista de productos en formato JSON, o un error 400 si el identificador es nulo o inválido.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idEmpresa") int idEmpresa) {

        // Validación de seguridad para asegurar que la petición incluya la empresa correspondiente.
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa.\"}").build();
        }

        // Consulta a la capa de datos para obtener los productos de la empresa.
        List<Producto> productos = dao.listarProductos(idEmpresa);
        return Response.ok(productos).build();
    }

    /**
     * OBTENER IMAGEN DEL PRODUCTO (GET)
     * Extrae y devuelve los bytes de la imagen asociada a un producto específico
     * para permitir su renderizado directo en elementos visuales del cliente.
     * Parametro id: El identificador único del producto a consultar.
     * Retorna: Los datos binarios de la imagen con estado 200, o un estado HTTP 404 si no se encuentra.
     */
    @GET
    @Path("/{id}/imagen")
    @Produces("image/jpeg")
    public Response obtenerImagen(@PathParam("id") int id) {
        // Recupera la información binaria de la imagen desde la base de datos.
        byte[] imagenBytes = dao.obtenerImagen(id);

        // Verifica si el producto contiene información gráfica válida.
        if (imagenBytes != null && imagenBytes.length > 0) {
            // Envía los datos binarios para su visualización.
            return Response.ok(imagenBytes).build();
        } else {
            // Notifica la ausencia de recursos gráficos para el producto solicitado.
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}