package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * CLASE DTO PARA CREACIÓN/EDICIÓN DE RUTINAS
 * Captura el payload JSON generado por el formulario modal "Nueva Rutina"
 * en el panel del Entrenador. Transfiere el nombre de la rutina y
 * la lista exacta de ejercicios seleccionados.
 */
public class NuevaRutinaDTO implements Serializable {
    private int idCliente;               // ID del cliente a quien se le asignará (0 si es plantilla genérica)
    private String nombreRutina;         // Nombre comercial del plan de entrenamiento
    private List<Integer> idsEjercicios; // Array con los identificadores de los ejercicios marcados en el Front

    public NuevaRutinaDTO() {} // Constructor vacío requerido para la des-serialización JSON

    // --- Getters y Setters ---

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombreRutina() { return nombreRutina; }
    public void setNombreRutina(String nombreRutina) { this.nombreRutina = nombreRutina; }

    public List<Integer> getIdsEjercicios() { return idsEjercicios; }
    public void setIdsEjercicios(List<Integer> idsEjercicios) { this.idsEjercicios = idsEjercicios; }
}