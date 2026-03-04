package com.mathew.gimnasio.util;

/**
 * Utilidad para construcción manual de JSON (escapado de cadenas).
 * Evita redundancia de métodos escape() en DAOs y controladores.
 */
public final class JsonUtil {

    private JsonUtil() {}

    /**
     * Escapa caracteres especiales para uso dentro de un valor JSON entre comillas.
     */
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
