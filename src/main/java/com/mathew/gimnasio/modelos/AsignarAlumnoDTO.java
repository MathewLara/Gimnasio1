package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * CLASE DTO PARA ASIGNAR ALUMNOS
 * Este objeto actúa como un "molde" o contrato JSON para recibir los datos desde el Frontend
 * cuando un Entrenador vincula a un nuevo cliente/socio a su cartera y le asigna una rutina.
 */
public class AsignarAlumnoDTO implements Serializable {
    private int idCliente;        // Identificador único del cliente/socio a vincular
    private int idEntrenador;     // Identificador único del entrenador que asume al alumno
    private int idRutinaAsignada; // ID de la rutina plantilla a clonar (Cambiado a int primitivo para evitar valores null)
    private String notas;         // Observaciones o historial médico (ej. lesiones, objetivos de hipertrofia)

    /**
     * Constructor vacío obligatorio.
     * Frameworks como JAX-RS o librerías de JSON lo necesitan obligatoriamente para
     * poder convertir el texto JSON que llega desde JavaScript a este objeto Java real.
     */
    public AsignarAlumnoDTO() {}

    // --- Getters y Setters ---
    // Permiten al backend acceder a la información empaquetada manteniendo el encapsulamiento de Java

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdEntrenador() { return idEntrenador; }
    public void setIdEntrenador(int idEntrenador) { this.idEntrenador = idEntrenador; }

    public int getIdRutinaAsignada() { return idRutinaAsignada; }
    public void setIdRutinaAsignada(int idRutinaAsignada) { this.idRutinaAsignada = idRutinaAsignada; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}