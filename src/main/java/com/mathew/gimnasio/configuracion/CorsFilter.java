/**
 * Author: Mathew Lara
 * Fecha: 07/06/2026
 */
package com.mathew.gimnasio.configuracion;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * FILTRO DE SEGURIDAD: CORS (Cross-Origin Resource Sharing)
 * Intercepta todas las peticiones HTTP entrantes y salientes para gestionar las políticas de origen cruzado.
 * Es fundamental para permitir que el frontend (JavaScript) consuma la API REST sin ser bloqueado por las políticas de seguridad del navegador.
 */
@Provider
@PreMatching // La anotación indica que el filtro se ejecuta globalmente, antes de que el servidor resuelva la ruta final (endpoint)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * FILTRO DE PETICIÓN (Request Filter)
     * Intercepta las solicitudes antes de que lleguen a los controladores.
     * Su objetivo principal es identificar y resolver las peticiones "Preflight" (OPTIONS) que hacen los navegadores por seguridad.
     * * Parámetro requestContext: Contexto de la solicitud HTTP entrante que contiene cabeceras y métodos.
     * * Excepciones:
     * Lanza IOException si ocurre un error de lectura durante la intercepción de la solicitud.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Verifica si la petición es un metodo OPTIONS (Preflight request de CORS)
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            // Aborta la cadena de ejecución normal y responde inmediatamente con un código 200 OK al navegador
            requestContext.abortWith(Response.status(Response.Status.OK).build());
        }
    }

    /**
     * FILTRO DE RESPUESTA (Response Filter)
     * Intercepta las respuestas emitidas por los controladores antes de que lleguen al cliente.
     * Inyecta las cabeceras HTTP necesarias para autorizar la comunicación desde distintos dominios.
     * * Parámetro requestContext: Contexto de la solicitud original.
     * Parámetro responseContext: Contexto de la respuesta que se enviará al cliente, donde se inyectan los headers.
     * * Excepciones:
     * Lanza IOException si ocurre un error de escritura al procesar la respuesta.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Permite peticiones desde cualquier origen (modificable a dominios específicos en producción estricta)
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");

        // Especifica qué cabeceras HTTP están permitidas en las solicitudes entrantes del cliente
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");

        // Habilita el soporte para el envío de credenciales cruzadas entre distintos dominios
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");

        // Define los métodos HTTP que la API expone y acepta públicamente
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}