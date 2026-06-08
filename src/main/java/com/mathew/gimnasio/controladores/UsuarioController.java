/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.UsuarioDAO;
import com.mathew.gimnasio.modelos.Usuario;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Random;

/**
 * CONTROLADOR DE USUARIOS
 * Gestiona las operaciones administrativas relacionadas con las cuentas de los usuarios.
 * Permite al personal autorizado realizar operaciones CRUD (creación, lectura, actualización
 * y eliminación) de manera directa sobre los registros del sistema.
 */
@Path("/usuarios")
public class UsuarioController {

    private UsuarioDAO dao = new UsuarioDAO();

    /**
     * OBTENER TODOS LOS USUARIOS (GET)
     * Recupera la lista completa de clientes y personal registrados en el sistema
     * para la alimentación de tablas y reportes en el panel administrativo.
     * Retorna: Respuesta HTTP 200 con la lista de usuarios en formato JSON.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarUsuarios() {
        // Ejecuta la consulta general a la base de datos
        List<Usuario> lista = dao.listar();
        return Response.ok(lista).build();
    }

    /**
     * OBTENER UN USUARIO POR ID (GET)
     * Recupera los detalles específicos de un usuario mediante su identificador único.
     * Parametro id: El número de identificación del usuario en la base de datos.
     * Retorna: El objeto Usuario en formato JSON, o un estado 404 si el registro no existe.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerUsuario(@PathParam("id") int id) {
        Usuario u = dao.obtenerPorId(id);

        // Validación de existencia del registro en la base de datos
        if (u != null) {
            return Response.ok(u).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Usuario no encontrado").build();
        }
    }

    /**
     * CREAR NUEVO USUARIO (POST)
     * Facilita la creación manual de una cuenta de usuario por parte de un administrador,
     * generando automáticamente un código de verificación temporal para cumplir
     * con los requerimientos estructurales de la capa de datos.
     * Parametro nuevoUsuario: El objeto estructurado con los datos ingresados en el formulario.
     * Retorna: Confirmación HTTP indicando éxito en la creación o error por duplicidad.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearUsuario(Usuario nuevoUsuario) {

        // 1. Validación de Integridad de Datos
        // Garantiza que la petición contenga un payload válido y con los campos requeridos
        if (nuevoUsuario == null) {
            return Response.status(400).entity("{\"mensaje\": \"No se enviaron datos.\"}").build();
        }
        if (nuevoUsuario.getUsuario() == null || nuevoUsuario.getUsuario().trim().isEmpty()) {
            return Response.status(400).entity("{\"mensaje\": \"El nombre de usuario es obligatorio.\"}").build();
        }

        // 2. Validación de Políticas de Seguridad
        if (nuevoUsuario.getContrasena() == null || nuevoUsuario.getContrasena().length() < 5) {
            return Response.status(400)
                    .entity("{\"mensaje\": \"La contraseña es muy débil. Debe tener al menos 5 caracteres.\"}").build();
        }

        // 3. Generación de Token Temporal
        // Implementa un código numérico de 6 dígitos para satisfacer las restricciones
        // de la base de datos al invocar el método de registro estándar.
        String codigoGenerado = String.format("%06d", new Random().nextInt(999999));

        // Ejecución de la transacción de guardado
        boolean exito = dao.registrarNuevoUsuario(nuevoUsuario, codigoGenerado);

        if (exito) {
            return Response.status(Response.Status.CREATED)
                    .entity("{\"mensaje\": \"Usuario creado con éxito\"}").build();
        } else {
            return Response.status(500)
                    .entity("{\"mensaje\": \"Error al crear usuario. Posiblemente el nombre o correo ya existen.\"}").build();
        }
    }

    /**
     * ACTUALIZAR USUARIO (PUT)
     * Procesa la modificación de los datos de un usuario existente, sobrescribiendo
     * la información en la base de datos según los parámetros enviados.
     * Parametro usuarioEditado: Objeto con los nuevos datos, que debe incluir el ID original.
     * Retorna: Estado de la transacción de actualización.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizarUsuario(Usuario usuarioEditado) {
        // Delega la ejecución del bloque UPDATE a la capa de acceso a datos
        boolean exito = dao.actualizar(usuarioEditado);

        if (exito) {
            return Response.ok("Usuario actualizado").build();
        } else {
            return Response.status(500).entity("Error al actualizar").build();
        }
    }

    /**
     * ELIMINAR USUARIO (DELETE)
     * Ejecuta el borrado permanente del registro de un cliente o empleado dentro del sistema.
     * Parametro id: El identificador único del usuario a eliminar.
     * Retorna: Confirmación de la operación de eliminación.
     */
    @DELETE
    @Path("/{id}")
    public Response eliminarUsuario(@PathParam("id") int id) {
        // Delega la ejecución del bloque DELETE a la capa de acceso a datos
        boolean exito = dao.eliminar(id);

        if (exito) {
            return Response.ok("Usuario eliminado").build();
        } else {
            return Response.status(500).entity("Error al eliminar").build();
        }
    }
}