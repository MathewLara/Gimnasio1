package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO para crear/actualizar rutinas (RF05: incluye series, reps, descanso)
 */
public class NuevaRutinaDTO implements Serializable {
    public int idCliente;
    public String nombreRutina;
    public List<Integer> idsEjercicios; // Compatibilidad: solo IDs
    public List<EjercicioEnRutinaDTO> ejercicios; // Detalle con series/reps/descanso
}
