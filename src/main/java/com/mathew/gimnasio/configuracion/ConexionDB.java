package com.mathew.gimnasio.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * CONEXIÓN A LA BASE DE DATOS
 * Esta clase es el puente central entre nuestra aplicación Java y PostgreSQL.
 * Sigue el patrón Singleton (o de acceso estático) para proporcionar conexiones
 * cada vez que un DAO necesita guardar o consultar información.
 */
public class ConexionDB {

    // Credenciales y ruta de nuestra base de datos PostgreSQL
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASS = "161825";

    /**
     * OBTENER CONEXIÓN
     * Se encarga de cargar el driver de la base de datos y establecer la conexión activa.
     * @return Un objeto Connection listo para ejecutar consultas SQL, o null si falla.
     */
    public static Connection getConnection() {
        try {
            // Registramos el driver de PostgreSQL para que Java sepa cómo comunicarse
            Class.forName("org.postgresql.Driver");

            // Intentamos abrir la puerta hacia la base de datos usando nuestras credenciales
            return DriverManager.getConnection(URL, USER, PASS);

        } catch (ClassNotFoundException | SQLException e) {
            // Si la base de datos está apagada o la contraseña es incorrecta, mostramos el error
            System.out.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}
