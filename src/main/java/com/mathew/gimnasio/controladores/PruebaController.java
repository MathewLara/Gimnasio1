/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RolDAO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR DE PRUEBA
 * Funciona como un componente de diagnóstico para el sistema.
 * Su función principal es validar la conexión con la base de datos
 * y confirmar la correcta entrega de datos por parte del servidor.
 */
@Path("/prueba")
public class PruebaController {

    /**
     * TEST DE CONEXIÓN Y ROLES (GET)
     * Realiza una validación inicial consultando la lista de roles del sistema.
     * Permite confirmar que la configuración de la base de datos y el enrutamiento operan correctamente.
     * Retorna: Una respuesta HTTP 200 con la lista de roles en formato JSON.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response test() {
        // Instancia la capa de acceso a datos para los roles.
        RolDAO dao = new RolDAO();

        // Retorna la colección de roles confirmando el estado operativo del servicio.
        return Response.ok(dao.obtenerRoles()).build();
    }
}