package com.mathew.gimnasio.controladores;

import com.mathew.gimnasio.dao.RolDAO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CONTROLADOR PRUEBA
 * * Este componente funciona como un "termómetro" para el sistema.
 * Su única función es confirmar que la conexión con la base de datos sea exitosa
 * y que el servidor pueda entregar datos correctamente.
 * Es el primer lugar donde miramos si algo falla en la configuración inicial.
 */
@Path("/prueba")
public class PruebaController {

    /**
     * TEST DE CONEXIÓN Y ROLES
     * * Este metodo realiza una prueba rápida: intenta traer la lista de roles
     * (como 'Administrador', 'Entrenador' o 'Cliente') desde la base de datos.
     * Si al entrar a esta URL ves la lista, ¡el sistema está vivo!
     * * URL: GET /api/prueba
     * @return Una respuesta con la lista de roles para confirmar que funciona.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON) // Le decimos al navegador que enviaremos una respuesta en JSON
    public Response test() {
        // Creamos una instancia del buscador de roles (RolDAO)
        RolDAO dao = new RolDAO();
        // Vamos a la base de datos, traemos los roles y los enviamos con un saludo de "OK" (Código 200)
        return Response.ok(dao.obtenerRoles()).build();
    }
}