package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.UsuarioDAO;
import com.mathew.gimnasio.modelos.Usuario;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Random; // Necesario para generar el código

/**
 * CONTROLADOR DE USUARIOS
 * Esta clase funciona como el panel de control administrativo de la aplicación.
 * A diferencia del AuthController (que es para que los clientes se registren solos),
 * este controlador permite a los administradores o recepcionistas gestionar
 * directamente los registros (ver todos, editar, crear manualmente o eliminar).
 */
@Path("/usuarios") // URL base: /api/usuarios
public class UsuarioController {

    private UsuarioDAO dao = new UsuarioDAO();

    /**
     * 1. OBTENER TODOS LOS USUARIOS (GET)
     * Solicita al DAO la lista completa de todos los clientes y personal
     * registrados en el gimnasio. Ideal para llenar tablas en el dashboard.
     * @return Respuesta HTTP 200 con la lista de usuarios en formato JSON.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarUsuarios() {
        // Obtenemos la lista desde la base de datos
        List<Usuario> lista = dao.listar();
        return Response.ok(lista).build();
    }

    /**
     * 2. OBTENER UN USUARIO POR ID (GET)
     * Busca los detalles específicos de un solo usuario utilizando su código
     * identificador único de la base de datos.
     * @param id El número de identificación del usuario.
     * @return El objeto Usuario en JSON, o un error 404 si el ID no existe.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerUsuario(@PathParam("id") int id) {
        Usuario u = dao.obtenerPorId(id);

        // Verificamos si la base de datos realmente encontró a alguien
        if (u != null) {
            return Response.ok(u).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Usuario no encontrado").build();
        }
    }

    /**
     * 3. CREAR NUEVO USUARIO (POST)
     * Permite a un administrador crear una cuenta manualmente.
     * Se adapta a las reglas de negocio del DAO generando un código de verificación por defecto.
     * @param nuevoUsuario El objeto con los datos tipeados por el administrador.
     * @return Respuesta HTTP indicando si se creó con éxito o si hubo un error.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearUsuario(Usuario nuevoUsuario) {

        // --- VALIDACIÓN 1: Datos obligatorios ---
        // Nos aseguramos de no procesar peticiones vacías o sin nombre de usuario
        if (nuevoUsuario == null) {
            return Response.status(400).entity("{\"mensaje\": \"No se enviaron datos.\"}").build();
        }
        if (nuevoUsuario.getUsuario() == null || nuevoUsuario.getUsuario().trim().isEmpty()) {
            return Response.status(400).entity("{\"mensaje\": \"El nombre de usuario es obligatorio.\"}").build();
        }

        // --- VALIDACIÓN 2: Fortaleza de Contraseña ---
        if (nuevoUsuario.getContrasena() == null || nuevoUsuario.getContrasena().length() < 5) {
            return Response.status(400)
                    .entity("{\"mensaje\": \"La contraseña es muy débil. Debe tener al menos 5 caracteres.\"}").build();
        }

        // --- CAMBIO PARA ARREGLAR EL ERROR ---
        // Generamos un código temporal aunque sea un registro administrativo,
        // para cumplir con el requisito del DAO (que exige siempre un código de 6 dígitos).
        String codigoGenerado = String.format("%06d", new Random().nextInt(999999));

        // Ahora pasamos AMBOS parámetros: el usuario Y el código generado
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
     * 4. ACTUALIZAR USUARIO (PUT)
     * Recibe los datos modificados de un usuario existente y sobrescribe
     * la información en la base de datos.
     * @param usuarioEditado Objeto con los nuevos datos (debe incluir el ID original).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizarUsuario(Usuario usuarioEditado) {
        // El DAO se encarga de hacer el UPDATE en SQL
        boolean exito = dao.actualizar(usuarioEditado);

        if (exito) {
            return Response.ok("Usuario actualizado").build();
        } else {
            return Response.status(500).entity("Error al actualizar").build();
        }
    }

    /**
     * 5. ELIMINAR USUARIO (DELETE)
     * Borra permanentemente el registro de un cliente o empleado del sistema.
     * @param id El identificador único del usuario a eliminar.
     */
    @DELETE
    @Path("/{id}")
    public Response eliminarUsuario(@PathParam("id") int id) {
        // El DAO ejecuta la sentencia DELETE en SQL
        boolean exito = dao.eliminar(id);

        if (exito) {
            return Response.ok("Usuario eliminado").build();
        } else {
            return Response.status(500).entity("Error al eliminar").build();
        }
    }
}