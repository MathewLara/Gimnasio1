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
    public boolean cancelado;

    // CAMPOS PARA LAS TARJETAS SUPERIORES
    public String ultimoIngreso;
    public String ultimaSalida;

    public static class AsistenciaSimple implements Serializable {
        public String fecha;
        public String hora;
        public String hora_salida; // NUEVO CAMPO

        // Constructor actualizado para recibir la salida
        public AsistenciaSimple(String f, String h, String hs) {
            this.fecha = f;
            this.hora = h;
            this.hora_salida = hs;
        }
    }

    public static class EjercicioSimple implements Serializable {
        public String nombre;
        public String seriesReps;
        public EjercicioSimple(String n, String s) { this.nombre = n; this.seriesReps = s; }
    }
}