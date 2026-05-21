package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.UsuarioDAO;
import com.mathew.gimnasio.modelos.Credenciales;
import com.mathew.gimnasio.modelos.Usuario;
import com.mathew.gimnasio.modelos.VerificacionRequest;
import com.mathew.gimnasio.servicios.EmailService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
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

        if (u.getIdRol() == 0) u.setIdRol(4);

        String codigoGenerado = String.format("%06d", new Random().nextInt(999999));

        boolean registrado = dao.registrarNuevoUsuario(u, codigoGenerado);

        if (registrado) {
            new EmailService().enviarCodigo(u.getEmail(), codigoGenerado);

            return Response.ok("{"
                    + "\"mensaje\": \"Registro exitoso. Revise su correo.\","
                    + "\"idUsuario\": " + u.getIdUsuario()
                    + "}").build();
        } else {
            return Response.status(409).entity("{\"mensaje\": \"El usuario o correo ya existen.\"}").build();
        }
    }

    /**
     * VERIFICACIÓN DE CUENTA POR CÓDIGO
     */
    @POST
    @Path("/verificar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarCuenta(VerificacionRequest request) {
        if (request.getEmail() == null || request.getCodigo() == null) return error("Faltan datos.");

        if (dao.validarCodigoPorEmail(request.getEmail(), request.getCodigo())) {
            return Response.ok("{\"mensaje\": \"Cuenta verificada.\"}").build();
        }
        return Response.status(401).entity("{\"mensaje\": \"Código incorrecto.\"}").build();
    }

    /**
     * INICIO DE SESIÓN
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Credenciales credenciales, @Context HttpServletRequest request) {

        String ipReal = request.getHeader("X-Forwarded-For");
        if (ipReal == null || ipReal.isEmpty()) {
            ipReal = request.getRemoteAddr();
        }

        if (ipReal != null && ipReal.length() > 48) {
            ipReal = ipReal.substring(0, 48);
        }

        Usuario usuario = dao.login(credenciales.getUsuario(), credenciales.getContrasena());

        if (usuario != null) {
            dao.registrarLogAcceso(usuario.getIdUsuario(), ipReal, "Exitoso");
            return Response.ok(usuario).build();
        } else {
            return Response.status(401).entity("{\"mensaje\":\"Credenciales incorrectas\"}").build();
        }
    }

    /**
     * ENDPOINT: AGREGAR USUARIO DESDE ADMIN
     */
    @POST
    @Path("/admin/usuarios")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearUsuarioAdmin(Usuario u) {
        if(dao.agregarPersonalAdmin(u)) {
            return Response.ok("{\"mensaje\": \"Creado exitosamente\"}").build();
        }
        return Response.status(400).entity("{\"mensaje\": \"Error al crear.\"}").build();
    }

    /**
     * ENDPOINT: EDITAR USUARIO DESDE ADMIN
     */
    @PUT
    @Path("/admin/usuarios/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarUsuarioAdmin(@PathParam("id") int id, Usuario u) {
        u.setIdUsuario(id);
        if(dao.editarPersonalAdmin(u)) {
            return Response.ok("{\"mensaje\": \"Actualizado exitosamente\"}").build();
        }
        return Response.status(400).entity("{\"mensaje\": \"Error al actualizar\"}").build();
    }

    /**
     * ENDPOINT PARA EL DASHBOARD DEL ADMINISTRADOR
     * Devuelve las métricas reales de la base de datos aisladas por empresa.
     */
    @GET
    @Path("/admin/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdminDashboard(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa en la petición.\"}").build();
        }
        String jsonReal = dao.getAdminStatsJSON(idEmpresa);
        return Response.ok(jsonReal).build();
    }

    /**
     * ENDPOINT: LISTAR TODOS LOS USUARIOS (AISLADO POR EMPRESA)
     */
    @GET
    @Path("/admin/usuarios")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsuariosAdmin(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa en la petición.\"}").build();
        }
        return Response.ok(dao.obtenerUsuariosParaAdminJSON(idEmpresa)).build();
    }

    /**
     * ENDPOINT: ELIMINADO LÓGICO / ACTIVACIÓN
     */
    @PUT
    @Path("/admin/usuarios/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstado(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        boolean exito = dao.cambiarEstadoUsuario(id, activo);
        if (exito) return Response.ok("{\"mensaje\": \"Estado actualizado correctamente\"}").build();
        return Response.status(400).entity("{\"mensaje\": \"Error al actualizar\"}").build();
    }

    /**
     * ENDPOINT PARA REPORTES GERENCIALES
     * Devuelve las métricas, ingresos y datos para los gráficos del dashboard filtrados por empresa.
     */
    @GET
    @Path("/admin/reportes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReportesAdmin(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa en la petición.\"}").build();
        }
        String jsonReportes = dao.getReportesJSON(idEmpresa);
        return Response.ok(jsonReportes).build();
    }

    /**
     * RF09: DESCARGAR REPORTE DE ACCESOS EN CSV
     */
    @GET
    @Path("/admin/reportes/accesos/csv")
    @Produces("text/csv")
    public Response descargarAccesosCSV(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa en la petición.\"}").build();
        }
        String csv = dao.getLogsAccesoCSV(idEmpresa);
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"reporte_auditoria_accesos.csv\"")
                .build();
    }

    /**
     * RF08: DESCARGAR REPORTE DE INGRESOS EN CSV
     */
    @GET
    @Path("/admin/reportes/ingresos/csv")
    @Produces("text/csv")
    public Response descargarIngresosCSV(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa en la petición.\"}").build();
        }
        String csv = dao.getReporteIngresosCSV(idEmpresa);
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"reporte_ingresos_economicos.csv\"")
                .build();
    }

    /**
     * FORMATEADOR DE ERRORES
     */
    private Response error(String mensaje) {
        return Response.status(400).entity("{\"mensaje\": \"" + mensaje + "\"}").build();
    }

    /**
     * ALGORITMO OFICIAL MÓDULO 10 PARA CÉDULA ECUATORIANA
     */
    private boolean esCedulaValida(String cedula) {
        if (cedula == null || !cedula.matches("\\d{10}")) return false;

        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 1 || provincia > 24) return false;

        int tercerDigito = Integer.parseInt(cedula.substring(2, 3));
        if (tercerDigito >= 6) return false;

        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;

        for (int i = 0; i < 9; i++) {
            int valor = Character.getNumericValue(cedula.charAt(i)) * coeficientes[i];
            if (valor >= 10) valor -= 9;
            suma += valor;
        }

        int digitoVerificador = Character.getNumericValue(cedula.charAt(9));
        int decenaSuperior = ((suma + 9) / 10) * 10;
        int resultado = decenaSuperior - suma;
        if (resultado == 10) resultado = 0;

        return resultado == digitoVerificador;
    }
}