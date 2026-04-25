package com.mathew.gimnasio.modelos;

/**
 * CLASE DE CREDENCIALES
 * Es un objeto de transferencia de datos (DTO) muy simple.
 * Su único propósito es capturar el JSON con el usuario y la contraseña
 * que se envía desde el formulario de inicio de sesión (Login) en el Frontend.
 */
public class Credenciales {
    private String usuario;    // El nombre de usuario ingresado en el campo de texto del login
    private String contrasena; // La contraseña en texto plano (se comparará con el Hash BCrypt en el backend)

    /**
     * Constructor vacío obligatorio.
     * Necesario para la des-serialización automática (transformar el JSON del fetch a este objeto).
     */
    public Credenciales() {}

    // --- Getters y Setters ---
    // Proveen acceso a las credenciales para validarlas en el AuthController

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}