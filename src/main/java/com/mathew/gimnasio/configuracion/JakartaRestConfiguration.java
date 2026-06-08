/**
 * Author: Mathew Lara
 * Fecha: 07/06/2026
 */
package com.mathew.gimnasio.configuracion;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * CONFIGURACIÓN PRINCIPAL DE LA API REST (JAX-RS)
 * Actúa como la puerta de entrada y configuración global de la arquitectura backend.
 * Registra la aplicación en el servidor de aplicaciones y define
 * la ruta base (context path) para todos los servicios web expuestos en el sistema.
 */
@ApplicationPath("/api")
public class JakartaRestConfiguration extends Application {

    // Al extender de la clase base 'Application', le indicamos a la especificación de Jakarta EE
    // que este proyecto agrupa y expone servicios RESTful.
    //
    // La anotación de ApplicationPath establece una regla de enrutamiento estricta:
    // Todos los controladores (endpoints) requerirán obligatoriamente el prefijo "/api" en su URL.
    // Ejemplo de acceso en producción: https://dominio.com/Gimnasio/api/[recurso]
}