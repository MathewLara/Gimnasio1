package com.mathew.gimnasio.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión a la base de datos.
 * Credenciales desde variables de entorno: DB_URL, DB_USER, DB_PASS.
 * No hardcodear en código; usar .env en local (no commitear) o config en el servidor.
 *
 * SSL: Si la URL no contiene parámetro "ssl", se añade sslmode=require automáticamente.
 * Esto es obligatorio en proveedores cloud (Render, Neon, Supabase).
 * Para desarrollo local sin SSL, añade "?sslmode=disable" explícitamente en DB_URL.
 */
public class ConexionDB {

    private static final String URL  = buildUrl(ConfiguracionEnv.get("DB_URL",  ""));
    private static final String USER = ConfiguracionEnv.get("DB_USER", "");
    private static final String PASS = ConfiguracionEnv.get("DB_PASS", "");

    /**
     * Si la URL ya contiene algún parámetro relacionado con SSL no la toca.
     * De lo contrario añade sslmode=require para que funcione en producción.
     */
    private static String buildUrl(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.contains("ssl")) return url;                  // ya tiene sslmode=... o ssl=true
        return url + (url.contains("?") ? "&" : "?") + "sslmode=require";
    }

    public static Connection getConnection() {
        try {
            if (URL == null || URL.isBlank() || USER.isBlank()) {
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