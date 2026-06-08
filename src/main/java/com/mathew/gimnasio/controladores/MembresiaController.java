/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.MembresiaDAO;
import com.mathew.gimnasio.modelos.Membresia;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * CONTROLADOR DE MEMBRESÍAS
 * Gestiona la consulta y listado del catálogo de planes de membresía
 * disponibles en el sistema según la empresa seleccionada.
 */
@Path("/membresias")
public class MembresiaController {

    private MembresiaDAO membresiaDAO = new MembresiaDAO();

    /**
     * OBTENER LISTADO DE MEMBRESÍAS (GET)
     * Recupera la colección de planes de membresía activos para una empresa específica,
     * facilitando su visualización en el sistema de ventas o registros.
     * Parametro idEmpresa: Identificador numérico de la empresa a consultar.
     * Retorna: Una lista en formato JSON de los objetos Membresia correspondientes a la sucursal.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerMembresias(@QueryParam("idEmpresa") int idEmpresa) {
        if (idEmpresa == 0) {
            return Response.status(400).entity("{\"mensaje\": \"Falta el ID de la empresa.\"}").build();
        }

        List<Membresia> membresias = membresiaDAO.listarPorEmpresa(idEmpresa);
        return Response.ok(membresias).build();
    }
}