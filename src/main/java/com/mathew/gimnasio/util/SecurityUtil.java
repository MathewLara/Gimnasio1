package com.mathew.gimnasio.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * UTILIDAD DE SEGURIDAD (SECURITY UTIL)
 * Esta clase es el escudo protector de los datos de los clientes del gimnasio.
 * Se encarga de encriptar las contraseñas utilizando el algoritmo BCrypt,
 * garantizando que NUNCA se guarden contraseñas en texto plano en la base de datos
 * (evitando vulnerabilidades en caso de un robo de información).
 */
public class SecurityUtil {

    /**
     * ENCRIPTAR CONTRASEÑA (HASHING)
     * Toma la contraseña real escrita por el usuario y la transforma en una
     * cadena de caracteres alfanuméricos indescifrable (hash).
     * * // Usar esto cuando CREAS un usuario nuevo (Registro de cliente o empleado)
     * * @param textoPlano La contraseña original tipeada (ej. "Gimnasio2026").
     * @return El hash encriptado de forma segura, listo para guardarse en la BD.
     */
    public static String encriptar(String textoPlano) {
        // BCrypt.gensalt() genera la "sal" aleatoria que fortalece la encriptación,
        // asegurando que cada hash sea único, incluso si dos personas usan la misma clave.
        return BCrypt.hashpw(textoPlano, BCrypt.gensalt());
    }

    /**
     * VERIFICAR CONTRASEÑA (MATCHING)
     * Como BCrypt es un método de encriptación de una sola vía (no se puede desencriptar),
     * este método toma la contraseña ingresada, la encripta internamente con la misma "sal",
     * y compara si el resultado matemático coincide con el hash guardado en la base de datos.
     * * // Usar esto cuando haces LOGIN (Inicio de sesión)
     * * @param textoPlano La contraseña que el usuario acaba de escribir en el formulario.
     * @param hashGuardado El hash largo y encriptado que extraemos de nuestra tabla 'usuarios'.
     * @return true si la contraseña es correcta, false si se equivocó de clave.
     */
    public static boolean verificar(String textoPlano, String hashGuardado) {
        // checkpw se encarga de hacer la validación segura previniendo ataques de tiempo
        return BCrypt.checkpw(textoPlano, hashGuardado);
    }
}