package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * Respuesta del login con JWT (RF02)
 */
public class LoginResponse implements Serializable {
    public String token;
    public Usuario usuario;

    public LoginResponse(String token, Usuario usuario) {
        this.token = token;
        this.usuario = usuario;
    }
}
