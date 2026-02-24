package com.mathew.gimnasio.configuracion;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CorsFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Permite que cualquier dominio (como tu localhost) se conecte
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        // Permite los encabezados necesarios para el login y envío de JSON
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        // Permite credenciales (opcional, pero buena práctica)
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        // Permite los métodos que usas en tu API
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}