package com.mathew.gimnasio.configuracion;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configuración principal de la API REST con Jersey (Tomcat).
 * ResourceConfig permite escanear automáticamente los controladores y filtros del paquete base.
 */
@ApplicationPath("/api")
public class JakartaRestConfiguration extends ResourceConfig {

    public JakartaRestConfiguration() {
        packages("com.mathew.gimnasio");
    }
}