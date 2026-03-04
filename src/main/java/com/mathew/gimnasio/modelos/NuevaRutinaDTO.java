package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO para crear/actualizar rutinas (RF05: incluye series, reps, descanso)
 */
public class NuevaRutinaDTO implements Serializable {
    private int idCliente;
    private String nombreRutina;
    private List<Integer> idsEjercicios;   // Compatibilidad: solo IDs
    private List<EjercicioEnRutinaDTO> ejercicios; // Detalle con series/reps/descanso

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public String getNombreRutina() { return nombreRutina; }
    public void setNombreRutina(String nombreRutina) { this.nombreRutina = nombreRutina; }
    public List<Integer> getIdsEjercicios() { return idsEjercicios; }
    public void setIdsEjercicios(List<Integer> idsEjercicios) { this.idsEjercicios = idsEjercicios; }
    public List<EjercicioEnRutinaDTO> getEjercicios() { return ejercicios; }
    public void setEjercicios(List<EjercicioEnRutinaDTO> ejercicios) { this.ejercicios = ejercicios; }
}
