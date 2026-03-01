package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.MembresiaDAO;
import com.mathew.gimnasio.modelos.MembresiaAsignacionDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Controlador de membresías (RF07)
 */
@Path("/membresias")
public class MembresiaController {

    private MembresiaDAO dao = new MembresiaDAO();

    /**
     * POST /membresias/{idCliente} - Asignar membresía a un cliente.
     * Body: { "idMembresia": 1 } o { "idTipoMembresia": 1 } (opcional: "duracionDias": 30)
     */
    @POST
    @Path("/{idCliente}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response asignarMembresia(@PathParam("idCliente") int idCliente, MembresiaAsignacionDTO dto) {
        if (dto == null || (dto.getIdMembresia() == null && dto.getIdTipoMembresia() == null)) {
            return Response.status(400).entity("{\"mensaje\":\"Debe indicar idMembresia o idTipoMembresia\"}").build();
        }
        boolean ok = dao.asignarMembresia(idCliente, dto);
        if (ok) return Response.ok("{\"mensaje\":\"Membresía asignada correctamente\"}").build();
        return Response.status(400).entity("{\"mensaje\":\"Error al asignar membresía\"}").build();
    }
}
