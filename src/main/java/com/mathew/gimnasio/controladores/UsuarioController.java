package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.UsuarioDAO;
import com.mathew.gimnasio.modelos.Usuario;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Random; // Necesario para generar el código

@Path("/usuarios") // URL base: /api/usuarios
public class UsuarioController {

    private UsuarioDAO dao = new UsuarioDAO();

    // 1. GET: Obtener todos
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarUsuarios() {
        List<Usuario> lista = dao.listar();
        return Response.ok(lista).build();
    }

    // 2. GET: Obtener uno por ID
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerUsuario(@PathParam("id") int id) {
        Usuario u = dao.obtenerPorId(id);
        if (u != null) {
            return Response.ok(u).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Usuario no encontrado").build();
        }
    }

    // 3. POST: Crear nuevo (CORREGIDO PARA EL NUEVO DAO)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearUsuario(Usuario nuevoUsuario) {

        // --- VALIDACIÓN 1: Datos obligatorios ---
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
        // para cumplir con el requisito del DAO.
        String codigoGenerado = String.format("%06d", new Random().nextInt(999999));

        // Ahora pasamos AMBOS parámetros: el usuario Y el código
        boolean exito = dao.registrarNuevoUsuario(nuevoUsuario, codigoGenerado);

        if (exito) {
            return Response.status(Response.Status.CREATED)
                    .entity("{\"mensaje\": \"Usuario creado con éxito\"}").build();
        } else {
            return Response.status(500)
                    .entity("{\"mensaje\": \"Error al crear usuario. Posiblemente el nombre o correo ya existen.\"}").build();
        }
    }

    // 4. PUT: Actualizar
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response actualizarUsuario(Usuario usuarioEditado) {
        boolean exito = dao.actualizar(usuarioEditado);
        if (exito) {
            return Response.ok("Usuario actualizado").build();
        } else {
            return Response.status(500).entity("Error al actualizar").build();
        }
    }

    // 5. DELETE: Borrar
    @DELETE
    @Path("/{id}")
    public Response eliminarUsuario(@PathParam("id") int id) {
        boolean exito = dao.eliminar(id);
        if (exito) {
            return Response.ok("Usuario eliminado").build();
        } else {
            return Response.status(500).entity("Error al eliminar").build();
        }
    }
}