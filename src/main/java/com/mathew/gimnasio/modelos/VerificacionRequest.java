package com.mathew.gimnasio.modelos;

import java.io.Serializable;

public class VerificacionRequest implements Serializable {
    private int idUsuario; // Lo dejamos por compatibilidad, pero el importante es el email
    private String email;  // <--- NUEVO CAMPO IMPORTANTE
    private String codigo;

    public VerificacionRequest() {} // Constructor vacío obligatorio

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    // --- Getters y Setters para Email ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}