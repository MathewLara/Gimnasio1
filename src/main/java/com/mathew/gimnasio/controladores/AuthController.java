package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.UsuarioDAO;
import com.mathew.gimnasio.modelos.Credenciales;
import com.mathew.gimnasio.modelos.Usuario;
import com.mathew.gimnasio.modelos.VerificacionRequest;
import com.mathew.gimnasio.servicios.EmailService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Random;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

@Path("/auth")
public class AuthController {

    private UsuarioDAO dao = new UsuarioDAO();

    // Regex (Tus validaciones se mantienen intactas)
    private static final String REGEX_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$";
    private static final String REGEX_EMAIL = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    private static final String REGEX_TELEFONO = "^09\\d{8}$";

    @POST
    @Path("/registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarUsuario(Usuario u) {

        // ==========================================
        //  TUS VALIDACIONES (NO SE TOCAN)
        // ==========================================

        // 1. Validar Nombre y Apellido
        if (u.getNombre() == null || !Pattern.matches(REGEX_LETRAS, u.getNombre())) {
            return error("El nombre es obligatorio y solo debe contener letras.");
        }
        if (u.getApellido() == null || !Pattern.matches(REGEX_LETRAS, u.getApellido())) {
            return error("El apellido es obligatorio y solo debe contener letras.");
        }

        // 2. Validar Email
        if (u.getEmail() == null || !Pattern.matches(REGEX_EMAIL, u.getEmail())) {
            return error("Ingrese un correo electrónico válido.");
        }

        // 3. Validar Teléfono
        if (u.getTelefono() == null || !Pattern.matches(REGEX_TELEFONO, u.getTelefono())) {
            return error("El teléfono debe tener 10 dígitos y empezar con 09.");
        }

        // 4. VALIDACIÓN ESTRICTA DE FECHA (EDAD)
        if (u.getFechaNacimiento() == null || u.getFechaNacimiento().isEmpty()) {
            return error("La fecha de nacimiento es obligatoria.");
        }
        try {
            LocalDate fechaNac = LocalDate.parse(u.getFechaNacimiento());
            LocalDate ahora = LocalDate.now();

            if (fechaNac.isAfter(ahora)) {
                return error("¡No puedes nacer en el futuro! Revisa la fecha.");
            }

            int edad = Period.between(fechaNac, ahora).getYears();

            if (edad < 12) {
                return error("Debes tener al menos 12 años para registrarte. Tu edad actual: " + edad + " años.");
            }

        } catch (DateTimeParseException e) {
            return error("Formato de fecha inválido. Use AAAA-MM-DD.");
        }

        // 5. Validar Usuario
        if (u.getUsuario() == null || u.getUsuario().length() < 4) {
            return error("El usuario debe tener al menos 4 caracteres.");
        }
        if (u.getUsuario().contains(" ")) {
            return error("El nombre de usuario NO puede tener espacios.");
        }

        // 6. Validar Contraseña
        if (u.getContrasena() == null || u.getContrasena().length() < 5) {
            return error("La contraseña es muy débil. Debe tener mínimo 5 caracteres.");
        }

        // ==========================================
        //  AQUÍ ESTÁ EL CAMBIO CLAVE (TRANSACCIÓN)
        // ==========================================

        if (u.getIdRol() == 0) u.setIdRol(4);

        // PASO A: Generamos el código AQUÍ (antes de llamar a la base de datos)
        String codigoGenerado = String.format("%06d", new Random().nextInt(999999));

        // PASO B: Se lo pasamos al DAO junto con el usuario
        // El DAO guardará ambas cosas al mismo tiempo. Si falla uno, falla todo.
        boolean registrado = dao.registrarNuevoUsuario(u, codigoGenerado);

        if (registrado) {
            // PASO C: Si la BD dijo "OK", enviamos el correo
            new EmailService().enviarCodigo(u.getEmail(), codigoGenerado);

            return Response.ok("{"
                    + "\"mensaje\": \"Registro exitoso. Revise su correo.\","
                    + "\"idUsuario\": " + u.getIdUsuario()
                    + "}").build();
        } else {
            return Response.status(409).entity("{\"mensaje\": \"El usuario o correo ya existen.\"}").build();
        }
    }

    // --- Mismos métodos de siempre para verificar y login ---
    @POST
    @Path("/verificar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarCuenta(VerificacionRequest request) {
        if (request.getEmail() == null || request.getCodigo() == null) return error("Faltan datos.");

        // Aquí usamos la validación por Email que ya tienes en el DAO
        if (dao.validarCodigoPorEmail(request.getEmail(), request.getCodigo())) {
            return Response.ok("{\"mensaje\": \"Cuenta verificada.\"}").build();
        }
        return Response.status(401).entity("{\"mensaje\": \"Código incorrecto.\"}").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Credenciales credenciales) {
        Usuario u = dao.login(credenciales.getUsuario(), credenciales.getContrasena());
        if (u != null) {
            if (u.isActivo()) return Response.ok(u).build();
            return Response.status(403).entity("{\"mensaje\": \"Cuenta no verificada.\"}").build();
        }
        return Response.status(401).entity("{\"mensaje\": \"Credenciales incorrectas\"}").build();
    }

    private Response error(String mensaje) {
        return Response.status(400).entity("{\"mensaje\": \"" + mensaje + "\"}").build();
    }
}