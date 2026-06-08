/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

/**
 * DTO DE CREDENCIALES
 * Objeto de Transferencia de Datos diseñado exclusivamente para la intercepción
 * y encapsulamiento de los parámetros de autenticación durante el proceso de inicio de sesión.
 */
public class Credenciales {
    private String usuario;
    private String contrasena;

    /**
     * Constructor predeterminado.
     * Requerido para la correcta conversión automática desde el formato JSON
     * transmitido en el cuerpo de la petición HTTP hacia un objeto nativo de Java.
     */
    public Credenciales() {}

    // --- Getters y Setters ---

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}