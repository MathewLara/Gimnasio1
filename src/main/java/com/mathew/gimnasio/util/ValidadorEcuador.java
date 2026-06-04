package com.mathew.gimnasio.util;

import java.util.regex.Pattern;

public class ValidadorEcuador {

    // Regex para validar formato de correo electrónico
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    // Regex para validar teléfono en Ecuador (Celular empieza con 09 y tiene 10 dígitos, o fijo de 9 dígitos)
    private static final String TELEFONO_REGEX = "^09\\d{8}$|^0[2-8]\\d{7}$";

    public static boolean esCorreoValido(String correo) {
        if (correo == null || correo.trim().isEmpty()) return false;
        return Pattern.matches(EMAIL_REGEX, correo);
    }

    public static boolean esTelefonoValido(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) return false;
        return Pattern.matches(TELEFONO_REGEX, telefono);
    }

    /**
     * Validador profesional de RUC Ecuatoriano (13 dígitos)
     * Verifica longitud, código de provincia, tercer dígito y sufijo "001".
     */
    public static boolean esRucValido(String ruc) {
        if (ruc == null || ruc.length() != 13 || !ruc.matches("\\d+")) {
            return false;
        }

        // El RUC en Ecuador casi siempre termina en 001 para la oficina principal
        if (!ruc.endsWith("001")) {
            return false;
        }

        int provincia = Integer.parseInt(ruc.substring(0, 2));
        // Las provincias en Ecuador van del 01 al 24 (y 30 para ecuatorianos en el exterior)
        if ((provincia < 1 || provincia > 24) && provincia != 30) {
            return false;
        }

        int tercerDigito = Integer.parseInt(ruc.substring(2, 3));
        // El tercer dígito debe ser menor a 6 (Persona Natural), 6 (Pública) o 9 (Privada)
        if (tercerDigito < 0 || tercerDigito == 7 || tercerDigito == 8) {
            return false;
        }

        return true;
    }
}