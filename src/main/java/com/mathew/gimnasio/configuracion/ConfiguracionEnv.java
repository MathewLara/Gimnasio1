package com.mathew.gimnasio.configuracion;

/**
 * Lee configuración desde variables de entorno.
 * No subir credenciales al repositorio: definir en el servidor o en .env (no commitear).
 */
public final class ConfiguracionEnv {

    private ConfiguracionEnv() {}

    public static String get(String envVar, String valorPorDefecto) {
        String v = System.getenv(envVar);
        return (v != null && !v.isBlank()) ? v.trim() : valorPorDefecto;
    }

    public static String getRequired(String envVar) {
        String v = System.getenv(envVar);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Variable de entorno requerida: " + envVar);
        return v.trim();
    }
}
