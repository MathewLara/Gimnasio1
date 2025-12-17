package com.mathew.gimnasio.modelos;

import java.io.Serializable;

public class VerificacionRequest implements Serializable {
    private int idUsuario;
    private String codigo;

    public VerificacionRequest() {} // Constructor vacío obligatorio

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}