package com.mathew.gimnasio.modelos;
import java.io.Serializable;
import java.util.List;

public class ResumenClienteDTO implements Serializable {
    public String nombreCompleto;
    public String email;
    public String telefono;
    public List<AsistenciaSimple> historialAsistencias;
    public String nombreRutina;
    public String entrenador;
    public List<EjercicioSimple> ejercicios;
    public boolean rutinaTerminadaHoy;
    public String nombrePlan;
    public Double precioPlan;
    public String fechaVencimiento;
    public String estadoMembresia;

    public static class AsistenciaSimple implements Serializable {
        public String fecha;
        public String hora;
        public AsistenciaSimple(String f, String h) { this.fecha = f; this.hora = h; }
    }
    public static class EjercicioSimple implements Serializable {
        public String nombre;
        public String seriesReps;
        public EjercicioSimple(String n, String s) { this.nombre = n; this.seriesReps = s; }
    }
}