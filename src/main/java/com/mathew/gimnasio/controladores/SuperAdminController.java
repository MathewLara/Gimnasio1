/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.SuperAdminDAO;
import com.mathew.gimnasio.modelos.Empresa;
import com.mathew.gimnasio.util.ValidadorEcuador;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * CONTROLADOR SUPERADMINISTRADOR (SAAS)
 * Gestiona el nivel más alto del sistema, permitiendo la administración
 * del modelo Multi-Tenant. Controla la creación de nuevas franquicias o gimnasios (Empresas)
 * y la asignación de sus respectivos administradores principales.
 */
@Path("/superadmin")
public class SuperAdminController {

    private SuperAdminDAO dao = new SuperAdminDAO();

    /**
     * OBTENER DASHBOARD SUPERADMINISTRADOR (GET)
     * Recupera las estadísticas globales del sistema completo,
     * abarcando la totalidad de empresas y usuarios registrados.
     * Retorna: JSON estructurado con los KPI globales.
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard() {
        return Response.ok(dao.getDashboardJSON()).build();
    }

    /**
     * LISTAR EMPRESAS REGISTRADAS (GET)
     * Extrae el catálogo completo de todos los gimnasios o sucursales
     * que operan actualmente dentro de la plataforma.
     * Retorna: JSON Array con la información de las empresas.
     */
    @GET
    @Path("/empresas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEmpresas() {
        return Response.ok(dao.getEmpresasJSON()).build();
    }

    /**
     * REGISTRAR NUEVA EMPRESA (POST)
     * Da de alta un nuevo gimnasio en el sistema, aplicando validaciones estrictas
     * de negocio y formatos ecuatorianos (como el RUC) para evitar registros basura.
     * Parametro empresa: Objeto con los datos institucionales de la nueva sucursal.
     * Retorna: Estado de la operación de creación en la base de datos.
     */
    @POST
    @Path("/empresas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearEmpresa(Empresa empresa) {

        // 1. Validación de campos nulos o vacíos (excluimos correo porque la tabla empresas no lo tiene)
        if (empresa.getNombre() == null || empresa.getNombre().trim().isEmpty() ||
                empresa.getRuc() == null || empresa.getRuc().trim().isEmpty() ||
                empresa.getTelefono() == null || empresa.getTelefono().trim().isEmpty() ||
                empresa.getDireccion() == null || empresa.getDireccion().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"Todos los campos (nombre, ruc, teléfono, dirección) son obligatorios.\"}")
                    .build();
        }

        // 2. Validación estricta de Formatos usando ValidadorEcuador
        if (!ValidadorEcuador.esRucValido(empresa.getRuc())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"El RUC ingresado no tiene un formato válido para Ecuador.\"}")
                    .build();
        }

        if (!ValidadorEcuador.esTelefonoValido(empresa.getTelefono())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"mensaje\": \"El teléfono debe contener solo números válidos (Ej: 09XXXXXXXX o 02XXXXXXX).\"}")
                    .build();
        }

        // 3. Validación de Reglas de Negocio (Duplicados en Base de Datos)
        if (dao.existeRuc(empresa.getRuc())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"mensaje\": \"Este RUC ya se encuentra registrado en el sistema.\"}")
                    .build();
        }

        if (dao.existeNombreEmpresa(empresa.getNombre())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"mensaje\": \"Ya existe un gimnasio registrado con ese nombre exacto.\"}")
                    .build();
        }

        if (dao.existeTelefono(empresa.getTelefono())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"mensaje\": \"Este número de teléfono ya está asociado a otra empresa.\"}")
                    .build();
        }

        if (dao.existeDireccion(empresa.getDireccion())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"mensaje\": \"Ya existe una empresa registrada en esta dirección exacta.\"}")
                    .build();
        }

        // 4. Si pasa todas las validaciones, guardamos en base de datos
        boolean exito = dao.registrarEmpresa(empresa);

        if (exito) {
            return Response.status(Response.Status.CREATED)
                    .entity("{\"mensaje\": \"Empresa registrada exitosamente.\"}")
                    .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"mensaje\": \"Ocurrió un error interno al guardar la empresa en la base de datos.\"}")
                    .build();
        }
    }

    /**
     * ACTUALIZAR DATOS DE EMPRESA (PUT)
     * Modifica la información institucional de una sucursal existente.
     * Parametro id: Identificador único de la empresa.
     * Parametro data: Mapa con los campos actualizados.
     * Retorna: Confirmación de la actualización en base de datos.
     */
    @PUT
    @Path("/empresas/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarEmpresa(@PathParam("id") int id, Map<String, String> data) {
        boolean ok = dao.editarEmpresa(id, data.get("nombre"), data.get("ruc"), data.get("telefono"), data.get("direccion"));
        if(ok) return Response.ok("{\"mensaje\":\"Empresa actualizada\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error al actualizar\"}").build();
    }

    /**
     * CAMBIAR ESTADO DE EMPRESA (PUT)
     * Desactiva (soft-delete) o reactiva una empresa completa, bloqueando
     * el acceso de todos sus usuarios sin eliminar la información física.
     * Parametro id: Identificador de la empresa.
     * Parametro activo: Nuevo estado lógico a establecer.
     * Retorna: Confirmación del cambio de estado.
     */
    @PUT
    @Path("/empresas/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoEmpresa(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        dao.cambiarEstadoEmpresa(id, activo);
        return Response.ok("{\"mensaje\":\"Estado actualizado\"}").build();
    }

    /**
     * LISTAR ADMINISTRADORES DEL SISTEMA (GET)
     * Recupera a todos los usuarios con rol de administrador, detallando
     * a qué empresa están asignados para gestionar su acceso.
     * Retorna: JSON Array de administradores de franquicias.
     */
    @GET
    @Path("/administradores")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdministradores() {
        return Response.ok(dao.getAdministradoresJSON()).build();
    }

    /**
     * CREAR ADMINISTRADOR PARA UNA EMPRESA (POST)
     * Asigna un nuevo usuario principal (dueño/gerente) a una sucursal específica
     * permitiéndole gestionar el gimnasio de forma independiente.
     * Parametro data: Mapa con la información personal y la empresa a asignar.
     * Retorna: Estado de la operación o error si hay duplicidad de credenciales.
     */
    @POST
    @Path("/administradores")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearAdmin(Map<String, String> data) {
        int idEmpresa = Integer.parseInt(String.valueOf(data.get("idEmpresa")));
        boolean ok = dao.guardarAdmin(idEmpresa, data.get("nombre"), data.get("apellido"), data.get("usuario"), data.get("contrasena"));
        if(ok) return Response.ok("{\"mensaje\":\"Dueño creado\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error (Usuario duplicado)\"}").build();
    }

    /**
     * ACTUALIZAR ADMINISTRADOR DE FRANQUICIA (PUT)
     * Modifica los datos o credenciales de un administrador existente,
     * incluyendo la posibilidad de reasignarlo a otra empresa.
     * Parametro id: Identificador del usuario administrador.
     * Parametro data: Campos de información a actualizar.
     * Retorna: Confirmación de modificación exitosa.
     */
    @PUT
    @Path("/administradores/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editarAdmin(@PathParam("id") int id, Map<String, String> data) {
        int idEmpresa = Integer.parseInt(String.valueOf(data.get("idEmpresa")));
        boolean ok = dao.editarAdmin(id, idEmpresa, data.get("nombre"), data.get("apellido"), data.get("usuario"), data.get("contrasena"));
        if(ok) return Response.ok("{\"mensaje\":\"Dueño actualizado\"}").build();
        return Response.status(500).entity("{\"mensaje\":\"Error al actualizar\"}").build();
    }

    /**
     * CAMBIAR ESTADO DE ADMINISTRADOR (PUT)
     * Suspende o rehabilita el acceso de un administrador principal al sistema.
     * Parametro id: Identificador del administrador.
     * Parametro activo: Booleano que define si el administrador tiene acceso.
     * Retorna: Confirmación de la actualización del estado.
     */
    @PUT
    @Path("/administradores/{id}/estado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cambiarEstadoAdmin(@PathParam("id") int id, @QueryParam("activo") boolean activo) {
        dao.cambiarEstadoAdmin(id, activo);
        return Response.ok("{\"mensaje\":\"Estado actualizado\"}").build();
    }
}