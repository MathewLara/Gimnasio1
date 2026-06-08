/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * DTO PARA ASIGNACIÓN DE ALUMNOS
 * Estructura de datos serializable que actúa como contrato para la recepción de
 * cargas útiles (payloads) JSON desde la interfaz de usuario. Centraliza la información
 * necesaria para que el motor de base de datos vincule un cliente a la cartera de un entrenador.
 */
public class AsignarAlumnoDTO implements Serializable {
    private int idCliente;
    private int idEntrenador;
    private int idRutinaAsignada;
    private String notas;

    /**
     * Constructor predeterminado.
     * Requerido estructuralmente por los motores de serialización y deserialización de Jakarta EE
     * para instanciar el objeto de forma automática antes de poblar sus atributos.
     */
    public AsignarAlumnoDTO() {}

    // --- Getters y Setters ---

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdEntrenador() { return idEntrenador; }
    public void setIdEntrenador(int idEntrenador) { this.idEntrenador = idEntrenador; }

    public int getIdRutinaAsignada() { return idRutinaAsignada; }
    public void setIdRutinaAsignada(int idRutinaAsignada) { this.idRutinaAsignada = idRutinaAsignada; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}