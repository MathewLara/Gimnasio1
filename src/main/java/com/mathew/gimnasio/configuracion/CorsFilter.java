package com.mathew.gimnasio.configuracion;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * FILTRO CORS (CROSS-ORIGIN RESOURCE SHARING)
 * Esta clase es vital para que nuestro frontend (HTML/JS) pueda hablar con este backend.
 * Como el frontend y el backend corren en puertos distintos, los navegadores
 * bloquean la conexión por seguridad. Este filtro le dice al navegador:
 * "Tranquilo, esta petición es de confianza, déjala pasar".
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    /**
     * AÑADIR CABECERAS DE PERMISO
     * Este método intercepta cada respuesta que sale del servidor hacia el cliente
     * y le estampa los "sellos" (cabeceras) de autorización.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        // 1. ¿A quién le permitimos conectarse? (A cualquier origen usando "*")
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");

        // 2. ¿Qué tipos de datos y permisos ocultos aceptamos? (JSON, autorizaciones, etc.)
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");

        // 3. ¿Permitimos el envío de credenciales o cookies? (Sí)
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");

        // 4. ¿Qué acciones (verbos HTTP) están permitidas en nuestra API?
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}