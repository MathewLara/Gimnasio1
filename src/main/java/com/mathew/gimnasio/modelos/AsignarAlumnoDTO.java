package com.mathew.gimnasio.modelos;

import java.io.Serializable;

public class AsignarAlumnoDTO implements Serializable {
    private int idCliente;
    private int idEntrenador;
    private int idRutinaAsignada; // Cambiado a int primitivo
    private String notas;

    // Constructor vacío obligatorio para que Java convierta el JSON
    public AsignarAlumnoDTO() {}

    // Getters y Setters
    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdEntrenador() { return idEntrenador; }
    public void setIdEntrenador(int idEntrenador) { this.idEntrenador = idEntrenador; }

    public int getIdRutinaAsignada() { return idRutinaAsignada; }
    public void setIdRutinaAsignada(int idRutinaAsignada) { this.idRutinaAsignada = idRutinaAsignada; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}