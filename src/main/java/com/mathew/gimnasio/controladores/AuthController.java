/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
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
 * Esta clase es el guardia de seguridad principal del sistema.
 * Me encargo de recibir a los usuarios nuevos, validar meticulosamente que sus datos sean reales
 * (como la cédula ecuatoriana y su edad), y coordinar el envío del correo de verificación.
 */
@Path("/auth")
public class AuthController {

    private UsuarioDAO dao = new UsuarioDAO();

    // Uso expresiones regulares para asegurar formatos de texto precisos y evitar inyecciones o datos basura
    private static final String REGEX_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$";
    private static final String REGEX_EMAIL = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    private static final String REGEX_TELEFONO = "^09\\d{8}$";

    /**
     * REGISTRO DE NUEVO CLIENTE (POST)
     * Recibo los datos del formulario frontend y los paso por múltiples filtros estrictos
     * antes de intentar guardarlos en la base de datos para garantizar la integridad de la información.
     * Parametro u: Objeto Usuario con la información ingresada en el registro.
     * Retorna: Una respuesta HTTP 200 si es exitoso, o 400/409 si hay errores en la validación de datos.
     */
    @POST
    @Path("/registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarUsuario(Usuario u) {

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

        // 6. VALIDACIÓN ESTRICTA DE FECHA (EDAD)
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

        // 7. Validar Nombre de Usuario (sin espacios y mínimo 4 letras)
        if (u.getUsuario() == null || u.getUsuario().length() < 4) {
            return error("El usuario debe tener al menos 4 caracteres.");
        }
        if (u.getUsuario().contains(" ")) {
            return error("El nombre de usuario NO puede tener espacios.");
        }

        // 8. Validar Contraseña (duplicado preventivo, requiere mínimo 5)
        if (u.getContrasena() == null || u.getContrasena().length() < 5) {
            return error("La contraseña es muy débil. Debe tener mínimo 5 caracteres.");
        }

        // Asignación de rol por defecto si no viene especificado (Rol 4 = Cliente)
        if (u.getIdRol() == 0) u.setIdRol(4);

        // Generación de código de verificación de 6 dígitos
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
     * VERIFICACIÓN DE CUENTA POR CÓDIGO (POST)
     * Valido que el código ingresado por el usuario coincida con el enviado a su correo electrónico.
     * Parametro request: Objeto que contiene el email y el código a verificar.
     * Retorna: Confirmación de cuenta verificada o error si el código es incorrecto.
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
     * INICIO DE SESIÓN (POST)
     * Proceso el login del usuario y registro la IP desde la cual se está conectando por motivos de auditoría.
     * Parametro credenciales: Usuario y contraseña ingresados.
     * Parametro request: Contexto HTTP para extraer la dirección IP real.
     * Retorna: Datos del usuario si el login es correcto, o error 401 si falla.
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Credenciales credenciales, @Context HttpServletRequest request) {

        // Obtener la IP real del cliente, considerando proxies inversos o balanceadores de carga
        String ipReal = request.getHeader("X-Forwarded-For");
        if (ipReal == null || ipReal.isEmpty()) {
            ipReal = request.getRemoteAddr();
        }

        // Truncar la IP si excede la longitud permitida en la base de datos
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
     * AGREGAR USUARIO DESDE ADMIN (POST)
     * Permito a los administradores crear cuentas de personal directamente en el sistema.
     * Parametro u: Objeto con los datos del nuevo usuario.
     * Retorna: Estado de la operación de creación.
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
     * EDITAR USUARIO DESDE ADMIN (PUT)
     * Permito la modificación de datos de un usuario existente desde el panel administrativo.
     * Parametro id: Identificador del usuario a editar.
     * Parametro u: Objeto con los datos actualizados.
     * Retorna: Confirmación de la actualización.
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
     * OBTENER MÉTRICAS DEL DASHBOARD DE ADMINISTRADOR (GET)
     * Devuelvo las métricas reales de la base de datos, aisladas y filtradas por la empresa solicitante.
     * Parametro idEmpresa: Identificador de la empresa actual.
     * Retorna: JSON estructurado con las estadísticas del dashboard.
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
     * LISTAR TODOS LOS USUARIOS AISLADOS POR EMPRESA (GET)
     * Recupero el listado completo de usuarios asegurando que solo pertenezcan a la empresa indicada.
     * Parametro idEmpresa: Identificador de la empresa.
     * Retorna: JSON Array con la lista de usuarios.
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
     * ELIMINADO LÓGICO / ACTIVACIÓN DE USUARIO (PUT)
     * Cambio el estado de un usuario (activo/inactivo) en lugar de borrarlo físicamente (soft-delete).
     * Parametro id: Identificador del usuario.
     * Parametro activo: Booleano que define el nuevo estado.
     * Retorna: Confirmación del cambio de estado.
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
     * OBTENER DATOS PARA REPORTES GERENCIALES (GET)
     * Devuelvo las métricas, ingresos y datos necesarios para alimentar los gráficos del sistema.
     * Parametro idEmpresa: Identificador de la empresa.
     * Retorna: JSON con la información financiera y estadística.
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
     * DESCARGAR REPORTE DE ACCESOS EN CSV (GET)
     * Genero y retorno un archivo CSV descargable con el log de auditoría de los accesos al sistema.
     * Parametro idEmpresa: Identificador de la empresa.
     * Retorna: Archivo de texto plano en formato CSV.
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
     * DESCARGAR REPORTE DE INGRESOS EN CSV (GET)
     * Genero y retorno un archivo CSV con el detalle de los ingresos económicos de la sucursal.
     * Parametro idEmpresa: Identificador de la empresa.
     * Retorna: Archivo de texto plano en formato CSV.
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
     * FORMATEADOR DE ERRORES (MÉTODO AUXILIAR)
     * Estandarizo las respuestas de error a formato JSON para facilitar su lectura en el frontend.
     */
    private Response error(String mensaje) {
        return Response.status(400).entity("{\"mensaje\": \"" + mensaje + "\"}").build();
    }

    /**
     * ALGORITMO OFICIAL MÓDULO 10 PARA CÉDULA ECUATORIANA
     * Implemento la validación matemática requerida por el Registro Civil de Ecuador
     * para verificar la autenticidad estructural de un número de cédula.
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