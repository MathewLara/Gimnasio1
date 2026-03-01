package com.mathew.gimnasio.filtros;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Filtro de roles: endpoints que requieren rol administrador (idRol = 1).
 * Debe ejecutarse después de JwtFilter (que ya ha validado el token y puesto idRol).
 */
@Provider
public class RoleFilter implements ContainerRequestFilter {

    private static final int ROL_ADMIN = 1;

    private static boolean requiereAdmin(String path) {
        if (path == null) return false;
        return path.startsWith("auth/admin/")
                || path.startsWith("reportes")
                || path.startsWith("logs/")
                || path.equals("accesos/estadisticas")
                || path.equals("pagos"); // GET /pagos lista general
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }
        String path = requestContext.getUriInfo().getPath();
        if (!requiereAdmin(path)) {
            return;
        }
        Integer idRol = (Integer) requestContext.getProperty("idRol");
        if (idRol == null || idRol != ROL_ADMIN) {
            requestContext.abortWith(
                    Response.status(403).entity("{\"mensaje\":\"Acceso denegado: se requiere rol administrador\"}").build());
        }
    }
}
