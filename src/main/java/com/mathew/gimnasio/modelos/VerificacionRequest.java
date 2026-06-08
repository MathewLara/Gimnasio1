/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * DTO DE SEGURIDAD: REQUEST DE VERIFICACIÓN
 * Maneja el flujo de validación de doble paso (2FA / Verificación de Email).
 * Captura el código OTP (One Time Password) que el usuario ingresa
 * tras recibir el correo de confirmación.
 */
public class VerificacionRequest implements Serializable {
    private int idUsuario; // (Legacy) Mantenido por compatibilidad
    private String email;  // Clave de búsqueda principal para localizar la cuenta a verificar
    private String codigo; // El token o PIN ingresado por el usuario

    public VerificacionRequest() {} // Constructor vacío obligatorio

    // --- Getters y Setters ---

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}