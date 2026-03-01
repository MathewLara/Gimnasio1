package com.mathew.gimnasio.filtros;

import com.mathew.gimnasio.util.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.List;

/**
 * FILTRO JWT (RF02)
 * Valida el token en rutas protegidas. Si no hay token o es inválido, retorna 401.
 */
@Provider
public class JwtFilter implements ContainerRequestFilter {

    /** Rutas públicas que NO requieren JWT */
    private static final List<String> RUTAS_PUBLICAS = List.of(
            "/api/auth/login",
            "/api/auth/registro",
            "/api/auth/verificar",
            "/api/auth/admin/login"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String fullPath = "/api/" + path;

        // OPTIONS siempre pasa (CORS)
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        // Rutas públicas
        for (String ruta : RUTAS_PUBLICAS) {
            if (fullPath.startsWith(ruta)) {
                return;
            }
        }

        // Rutas bajo /auth (login, registro, verificar) - públicas
        if (path != null && path.startsWith("auth/")) {
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
