package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * Detalle de ejercicio en rutina con series, reps y descanso (RF05)
 */
public class EjercicioEnRutinaDTO implements Serializable {
    public int idEjercicio;
    public String series;      // ej: "4"
    public String repeticiones; // ej: "12"
    public String descanso;     // ej: "60 seg"

    public int getIdEjercicio() { return idEjercicio; }
    public void setIdEjercicio(int idEjercicio) { this.idEjercicio = idEjercicio; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getRepeticiones() { return repeticiones; }
    public void setRepeticiones(String repeticiones) { this.repeticiones = repeticiones; }
    public String getDescanso() { return descanso; }
    public void setDescanso(String descanso) { this.descanso = descanso; }
}
