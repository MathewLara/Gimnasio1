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

/**
 * CONTROLADOR DE AUTENTICACIÓN
 * Esta clase es el "guardia de seguridad" principal del gimnasio.
 * Recibe a los usuarios nuevos, valida meticulosamente que sus datos sean reales
 * (como la cédula ecuatoriana y su edad), y coordina el envío del correo de verificación.
 */
@Path("/auth")
public class AuthController {

    private UsuarioDAO dao = new UsuarioDAO();

    // Regex (Tus validaciones se mantienen intactas)
    // Se usan expresiones regulares para asegurar formatos de texto precisos
    private static final String REGEX_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$";
    private static final String REGEX_EMAIL = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    private static final String REGEX_TELEFONO = "^09\\d{8}$";

    /**
     * REGISTRO DE NUEVO CLIENTE
     * Recibe los datos del formulario frontend y los pasa por múltiples filtros
     * antes de intentar guardarlos en la base de datos.
     * @param u Objeto Usuario con la información ingresada en el registro.
     * @return Una respuesta HTTP (200 si es exitoso, 400 o 409 si hay errores en los datos).
     */
    @POST
    @Path("/registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarUsuario(Usuario u) {

        // ==========================================
        //  TUS VALIDACIONES (NO SE TOCAN)
        // ==========================================

        // 1. Validar Nombre y Apellido: Solo letras y mínimo 3 caracteres
        if (u.getNombre() == null || u.getNombre().trim().length() < 3 || !Pattern.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", u.getNombre())) {
            return error("El nombre debe tener al menos 3 letras y no contener números.");
        }
        if (u.getApellido() == null || u.getApellido().trim().length() < 3 || !Pattern.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", u.getApellido())) {
            return error("El apellido debe tener al menos 3 letras y no contener números.");
        }

        // 2. Validar Cédula Ecuatoriana (Usando el algoritmo Módulo 10 al final del archivo)
        if (!esCedulaValida(u.getCedula())) {
            return error("La cédula ingresada no es válida o no corresponde a Ecuador.");
        }

        // 3. Validar Teléfono (al menos 9 dígitos)
        // Nota: En Ecuador los celulares tienen 10 (ej: 099...) y fijos 9 (ej: 022...). Aceptamos desde 9.
        if (u.getTelefono() == null || !u.getTelefono().matches("\\d{9,}")) {
            return error("El teléfono debe tener por lo menos 9 dígitos numéricos.");
        }

        // 4. Validar Contraseña (mínimo 6 caracteres)
        if (u.getContrasena() == null || u.getContrasena().length() < 6) {
            return error("La contraseña debe tener al menos 6 caracteres por seguridad.");
        }

        // 5. Validar Email mediante Regex
        if (u.getEmail() == null || !Pattern.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", u.getEmail())) {
            return error("Ingrese un correo electrónico válido.");
        }

        // 4. VALIDACIÓN ESTRICTA DE FECHA (EDAD)
        // Nos aseguramos de que el cliente tenga al menos 12 años y no ponga fechas irreales.
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

        // 5. Validar Nombre de Usuario (sin espacios y mínimo 4 letras)
        if (u.getUsuario() == null || u.getUsuario().length() < 4) {
            return error("El usuario debe tener al menos 4 caracteres.");
        }
        if (u.getUsuario().contains(" ")) {
            return error("El nombre de usuario NO puede tener espacios.");
        }

        // 6. Validar Contraseña (duplicado preventivo, requiere mínimo 5)
        if (u.getContrasena() == null || u.getContrasena().length() < 5) {
            return error("La contraseña es muy débil. Debe tener mínimo 5 caracteres.");
        }

        // ==========================================
        //  AQUÍ ESTÁ EL CAMBIO CLAVE (TRANSACCIÓN)
        // ==========================================

        // Asignamos el rol 4 (Cliente) por defecto si no viene ninguno especificado
        if (u.getIdRol() == 0) u.setIdRol(4);

        // PASO A: Generamos el código AQUÍ (antes de llamar a la base de datos)
        // Será un código de 6 números aleatorios para validar el correo
        String codigoGenerado = String.format("%06d", new Random().nextInt(999999));

        // PASO B: Se lo pasamos al DAO junto con el usuario
        // El DAO guardará ambas cosas al mismo tiempo. Si falla uno, falla todo.
        boolean registrado = dao.registrarNuevoUsuario(u, codigoGenerado);

        if (registrado) {
            // PASO C: Si la BD dijo "OK", enviamos el correo usando nuestro servicio
            new EmailService().enviarCodigo(u.getEmail(), codigoGenerado);

            return Response.ok("{"
                    + "\"mensaje\": \"Registro exitoso. Revise su correo.\","
                    + "\"idUsuario\": " + u.getIdUsuario()
                    + "}").build();
        } else {
            // Si retorna false, usualmente es porque violó alguna restricción UNIQUE (como email o cédula repetida)
            return Response.status(409).entity("{\"mensaje\": \"El usuario o correo ya existen.\"}").build();
        }
    }

    // --- Mismos métodos de siempre para verificar y login ---

    /**
     * VERIFICACIÓN DE CUENTA POR CÓDIGO
     * Compara el código que el usuario ingresó en la web con el que enviamos a su correo.
     * @param request Objeto que contiene el email y el código tipeado.
     */
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

    /**
     * INICIO DE SESIÓN
     * Verifica credenciales y el estado activo de la cuenta.
     * @param credenciales Objeto con el usuario y la contraseña.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Credenciales credenciales) {
        // Busca al usuario en la BD comparando las credenciales
        Usuario u = dao.login(credenciales.getUsuario(), credenciales.getContrasena());
        if (u != null) {
            // Solo permite el acceso si ya verificó su correo (isActivo == true)
            if (u.isActivo()) return Response.ok(u).build();
            return Response.status(403).entity("{\"mensaje\": \"Cuenta no verificada.\"}").build();
        }
        return Response.status(401).entity("{\"mensaje\": \"Credenciales incorrectas\"}").build();
    }
    /**
     * ENDPOINT PARA EL DASHBOARD DEL ADMINISTRADOR
     * Devuelve las métricas reales de la base de datos.
     */
    @GET
    @Path("/admin/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdminDashboard() {
        // Llamamos a la función que acabamos de crear en el DAO
        String jsonReal = dao.getAdminStatsJSON();
        return Response.ok(jsonReal).build();
    }

    /**
     * FORMATEADOR DE ERRORES
     * Método auxiliar para enviar mensajes de error siempre en el mismo formato JSON.
     */
    private Response error(String mensaje) {
        return Response.status(400).entity("{\"mensaje\": \"" + mensaje + "\"}").build();
    }

    /**
     * ALGORITMO OFICIAL MÓDULO 10 PARA CÉDULA ECUATORIANA
     * Realiza operaciones matemáticas con los primeros 9 dígitos de la cédula
     * para verificar si el resultado coincide con el décimo dígito (el verificador).
     * @param cedula String de 10 números ingresado por el usuario.
     * @return true si las matemáticas coinciden, false si es falsa o mal formada.
     */
    private boolean esCedulaValida(String cedula) {
        // Verifica longitud y que solo contenga números
        if (cedula == null || !cedula.matches("\\d{10}")) return false;

        // Verifica que la provincia sea válida (01 a 24)
        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 1 || provincia > 24) return false;

        // El tercer dígito para personas naturales debe ser menor a 6
        int tercerDigito = Integer.parseInt(cedula.substring(2, 3));
        if (tercerDigito >= 6) return false;

        // Multiplicadores oficiales del Registro Civil
        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;

        // Multiplicar cada dígito por su coeficiente y sumar los resultados
        for (int i = 0; i < 9; i++) {
            int valor = Character.getNumericValue(cedula.charAt(i)) * coeficientes[i];
            if (valor >= 10) valor -= 9; // Si el resultado es 10 o más, se le resta 9
            suma += valor;
        }

        // Extraer el dígito verificador y calcular el resultado final
        int digitoVerificador = Character.getNumericValue(cedula.charAt(9));
        int decenaSuperior = ((suma + 9) / 10) * 10;
        int resultado = decenaSuperior - suma;
        if (resultado == 10) resultado = 0;

        // Si el resultado de la operación coincide con el último número de la cédula, es real
        return resultado == digitoVerificador;
    }
}