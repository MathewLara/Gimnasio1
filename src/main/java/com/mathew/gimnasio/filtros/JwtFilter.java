package com.mathew.gimnasio.filtros;

import com.mathew.gimnasio.util.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Set;

/**
 * Filtro JWT: exige token válido en todas las rutas excepto las explícitamente públicas.
 * path = getUriInfo().getPath() es relativo al ApplicationPath ("/api"), ej: "auth/login", "clientes/1/dashboard".
 */
@Provider
public class JwtFilter implements ContainerRequestFilter {

    /** Rutas públicas exactas o prefijos (path sin /api/). Solo estas no requieren JWT. */
    private static final Set<String> RUTAS_PUBLICAS = Set.of(
            "auth/login",
            "auth/registro",
            "auth/verificar"
    );

    private static boolean esRutaPublica(String path) {
        if (path == null || path.isBlank()) return true;
        for (String ruta : RUTAS_PUBLICAS) {
            if (path.equals(ruta) || path.startsWith(ruta + "/")) return true;
        }
        return false;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (esRutaPublica(path)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        String token = JwtService.extraerToken(authHeader);

        if (token == null || token.isBlank()) {
            requestContext.abortWith(Response.status(401).entity("{\"mensaje\":\"Token requerido\"}").build());
            return;
        }

        try {
            Claims claims = JwtService.validarToken(token);
            requestContext.setProperty("idUsuario", Integer.parseInt(claims.getSubject()));
            requestContext.setProperty("idRol", claims.get("rol", Integer.class));
        } catch (Exception e) {
            requestContext.abortWith(Response.status(401).entity("{\"mensaje\":\"Token inválido o expirado\"}").build());
        }
    }
}
