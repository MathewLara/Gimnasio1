package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.UsuarioDAO;
import com.mathew.gimnasio.modelos.Credenciales;
import com.mathew.gimnasio.modelos.Usuario;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.mathew.gimnasio.servicios.EmailService; // OJO con el nombre del paquete
import java.util.Random;
import java.util.regex.Pattern;

import com.mathew.gimnasio.modelos.VerificacionRequest;

@Path("/auth") // La URL será: .../api/auth
public class AuthController {

    private UsuarioDAO dao = new UsuarioDAO();

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response iniciarSesion(Credenciales credenciales) {

        // --- NIVEL 1: VALIDACIÓN DE ENTRADA (Datos vacíos) ---
        if (credenciales == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"No se enviaron datos.\"}").build();
        }
        if (credenciales.getUsuario() == null || credenciales.getUsuario().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"El nombre de usuario es obligatorio.\"}").build();
        }
        if (credenciales.getContrasena() == null || credenciales.getContrasena().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"La contraseña es obligatoria.\"}").build();
        }

        // --- NIVEL 2: VALIDACIÓN DE CREDENCIALES (DAO) ---
        // Aquí el DAO busca al usuario y SecurityUtil verifica el hash BCrypt
        Usuario usuarioEncontrado = dao.login(credenciales.getUsuario(), credenciales.getContrasena());

        if (usuarioEncontrado == null) {
            // Si es null, es porque el usuario no existe O la contraseña está mal
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"mensaje\": \"Usuario o contraseña incorrectos.\"}").build();
        }

        // --- NIVEL 3: VALIDACIÓN DE CORREO ELECTRÓNICO ---
        // Buscamos el email en la BD
        String emailDestino = dao.obtenerEmail(usuarioEncontrado.getIdUsuario());

        // 3.1: Validar que el usuario tenga un email registrado
        if (emailDestino == null || emailDestino.trim().isEmpty()) {
            return Response.status(Response.Status.CONFLICT) // 409 Conflict
                    .entity("{\"mensaje\": \"El usuario es válido, pero no tiene un correo registrado para enviar el código.\"}").build();
        }

        // 3.2: Validar formato de correo (Regex) - Opcional pero recomendado
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!Pattern.matches(emailRegex, emailDestino)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"mensaje\": \"El correo registrado en la base de datos tiene un formato inválido: " + emailDestino + "\"}").build();
        }

        try {
            // 1. Generar código
            String codigo = String.format("%06d", new Random().nextInt(999999));

            // 2. Guardar en BD
            dao.guardarCodigo2FA(usuarioEncontrado.getIdUsuario(), codigo);

            // 3. Enviar correo
            EmailService emailService = new EmailService();
            emailService.enviarCodigo(emailDestino, codigo);

            // Respuesta Éxitosa
            return Response.ok("{\"mensaje\": \"Login correcto. Se ha enviado un código a " + emailDestino + "\"}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity("{\"mensaje\": \"Error interno enviando el correo: " + e.getMessage() + "\"}").build();
        }
    }
    @POST
    @Path("/verificar") // URL: .../api/auth/verificar
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarCodigo(VerificacionRequest datos) {

        // 1. Validar que vengan los datos básicos
        if (datos == null || datos.getCodigo() == null || datos.getCodigo().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"Faltan datos de verificación\"}").build();
        }

        // 2. Llamar al DAO para validar contra la base de datos
        boolean esValido = dao.validarCodigo2FA(datos.getIdUsuario(), datos.getCodigo());

        if (esValido) {
            // 3. ¡ÉXITO! Recuperamos al usuario para devolver sus datos al Frontend
            // Esto le sirve al otro grupo para saber qué menús mostrar (Rol)
            Usuario u = dao.obtenerPorId(datos.getIdUsuario());

            if (u != null) {
                u.setContrasena(null); // IMPORTANTE: Borramos la contraseña antes de enviarla
                return Response.ok(u).build();
            } else {
                return Response.serverError().entity("{\"mensaje\": \"Error recuperando usuario\"}").build();
            }
        } else {
            // 4. FALLO: El código expiró, es incorrecto o ya se usó
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"mensaje\": \"Código inválido, expirado o ya utilizado.\"}").build();
        }
    }
}