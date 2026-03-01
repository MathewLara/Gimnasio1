package com.mathew.gimnasio.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión a la base de datos.
 * Credenciales desde variables de entorno: DB_URL, DB_USER, DB_PASS.
 * No hardcodear en código; usar .env en local (no commitear) o config en el servidor.
 */
public class ConexionDB {

    private static final String URL = ConfiguracionEnv.get("DB_URL", "");
    private static final String USER = ConfiguracionEnv.get("DB_USER", "");
    private static final String PASS = ConfiguracionEnv.get("DB_PASS", "");

    public static Connection getConnection() {
        try {
            if (URL.isEmpty() || USER.isEmpty()) {
                System.err.println("Configura DB_URL, DB_USER y DB_PASS (variables de entorno).");
                return null;
            }
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return null;
        }
    }
}
