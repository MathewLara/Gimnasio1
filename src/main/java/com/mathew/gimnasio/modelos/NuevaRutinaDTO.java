/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO PARA CREACIÓN/EDICIÓN DE RUTINAS
 * Captura la carga útil (payload) JSON generada por los formularios de creación
 * de rutinas en el panel del entrenador. Transfiere el nombre de la rutina y
 * la lista exacta de identificadores de ejercicios seleccionados.
 */
public class NuevaRutinaDTO implements Serializable {
    private int idCliente;
    private String nombreRutina;
    private List<Integer> idsEjercicios;

    public NuevaRutinaDTO() {}

    // --- Getters y Setters ---

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombreRutina() { return nombreRutina; }
    public void setNombreRutina(String nombreRutina) { this.nombreRutina = nombreRutina; }

    public List<Integer> getIdsEjercicios() { return idsEjercicios; }
    public void setIdsEjercicios(List<Integer> idsEjercicios) { this.idsEjercicios = idsEjercicios; }
}