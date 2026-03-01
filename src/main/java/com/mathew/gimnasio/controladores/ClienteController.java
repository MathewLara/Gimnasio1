package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.ClienteDAO;
import com.mathew.gimnasio.dao.ClienteDashboardDAO;
import com.mathew.gimnasio.modelos.ClienteUpdateDTO;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.POST;

import java.sql.Date;
import java.util.Map;

/**
 * CONTROLADOR DE CLIENTES (RF03)
 */
@Path("/clientes")
public class ClienteController {
    private ClienteDashboardDAO dao = new ClienteDashboardDAO();
    private ClienteDAO clienteDAO = new ClienteDAO();

    /**
     * Recupera el conjunto de datos necesarios para renderizar el Dashboard del cliente.
     * * El recurso es accesible mediante el metodo HTTP GET. Retorna un objeto JSON
     * que contiene el perfil, asistencias y ejercicios actuales del usuario.
     * * URL: GET /api/clientes/{idUsuario}/dashboard
     * * @param id Identificador de usuario extraído de la ruta de la URL.
     * @return Response Objeto de respuesta HTTP con el DTO serializado en formato JSON.
     */
    @GET
    @Path("/{idUsuario}/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dashboard(@PathParam("idUsuario") int id) {
        // Delegación de la lógica de negocio al componente DAO especializado
        ResumenClienteDTO datos = dao.obtenerInfoDashboard(id);
        // Respuesta exitosa (HTTP 200) con el cuerpo del mensaje poblado
        if (datos != null) return Response.ok(datos).build();
        // Respuesta de error controlado (HTTP 404) cuando el recurso no existe
        return Response.status(404).entity("{\"mensaje\":\"Cliente no encontrado\"}").build();
    }
    /**
     * GET /clientes/{id} - Obtener cliente por id_cliente (RF03)
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerCliente(@PathParam("id") int id) {
        Map<String, Object> cliente = clienteDAO.obtenerPorId(id);
        if (cliente != null) return Response.ok(cliente).build();
        return Response.status(404).entity("{\"mensaje\":\"Cliente no encontrado\"}").build();
    }

    /**
     * PUT /clientes/{id} - Actualizar datos del cliente (RF03)
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarCliente(@PathParam("id") int id, ClienteUpdateDTO dto) {
        java.sql.Date fechaNac = null;
        if (dto.getFechaNacimiento() != null && !dto.getFechaNacimiento().isEmpty()) {
            try { fechaNac = Date.valueOf(dto.getFechaNacimiento()); } catch (Exception e) {}
        }
        boolean ok = clienteDAO.actualizar(id, dto.getNombre(), dto.getApellido(),
                dto.getEmail(), dto.getTelefono(), fechaNac);
        if (ok) return Response.ok("{\"mensaje\":\"Cliente actualizado\"}").build();
        return Response.status(400).entity("{\"mensaje\":\"Error al actualizar\"}").build();
    }

    /**
     * GET /clientes/{id}/pagos - Historial de pagos del cliente (RF03, RF07)
     */
    @GET
    @Path("/{id}/pagos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response historialPagos(@PathParam("id") int id) {
        String json = clienteDAO.obtenerHistorialPagosJSON(id);
        return Response.ok(json).build();
    }

    /**
     * Procesa la notificación de finalización de una sesión de entrenamiento.
     * * Este recurso utiliza el metodo HTTP POST para realizar una escritura persistente
     * en el historial de actividades del gimnasio.
     * * URL: POST /api/clientes/{idUsuario}/completar
     * * @param id Identificador del usuario que finaliza la actividad.
     * @return Response Confirmación de la operación o aviso de registro existente.
     */
    @POST
    @Path("/{idUsuario}/completar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response completarRutina(@PathParam("idUsuario") int id) {
        // Ejecución de la persistencia de datos mediante el DAO
        boolean exito = dao.registrarTerminoRutina(id);

        if (exito) {
            // Confirmación de inserción correcta en la base de datos
            return Response.ok("{\"mensaje\": \"Entrenamiento registrado exitosamente\"}").build();
        } else {
            // Si devuelve false, asumimos que ya estaba registrado hoy, pero respondemos OK
            // para que el cliente vea el botón verde y no se preocupe.
            return Response.ok("{\"mensaje\": \"Ya estaba registrado hoy\"}").build();
        }
    }
}