package com.mathew.gimnasio.configuracion;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * CONFIGURACIÓN PRINCIPAL DE LA API REST
 * Esta es la puerta de entrada principal de toda nuestra arquitectura backend.
 * Sin esta clase, el servidor no sabría que estamos creando una API RESTful.
 */
@ApplicationPath("/api")
public class JakartaRestConfiguration extends Application {

    // Al extender de 'Application' y usar la anotación '@ApplicationPath("/api")',
    // le indicamos al servidor que TODAS nuestras rutas web (endpoints)
    // deben empezar obligatoriamente con la palabra "/api" para mantener el orden.
    // Ejemplo: localhost:8080/Gimnasio/api/auth/login

}