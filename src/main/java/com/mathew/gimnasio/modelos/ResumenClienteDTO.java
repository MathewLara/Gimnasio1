package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO AGREGADOR: RESUMEN DE CLIENTE
 * Este es uno de los DTOs más críticos del sistema. Actúa como un "God Object" de solo lectura.
 * Su propósito es recolectar datos de 5 tablas diferentes (Usuarios, Clientes, Membresías,
 * Rutinas y Asistencias) y consolidarlos en un solo JSON. Esto permite que el Dashboard
 * del Cliente o el Kiosko carguen instantáneamente con una sola petición HTTP.
 */
public class ResumenClienteDTO implements Serializable {

    // --- Datos Personales y de Contacto ---
    public String nombreCompleto;
    public String email;
    public String telefono;

    // --- Estado Financiero (Membresía) ---
    public String nombrePlan;
    public Double precioPlan;
    public String fechaVencimiento;
    public String estadoMembresia;
    public boolean cancelado;

    // --- Telemetría de Accesos (Puerta) ---
    public String ultimoIngreso;
    public String ultimaSalida;
    public List<AsistenciaSimple> historialAsistencias; // Log de los últimos días

    // --- Datos Deportivos (Entrenamiento) ---
    public String nombreRutina;
    public String entrenador;
    public List<EjercicioSimple> ejercicios; // Lista de ejercicios a realizar
    public boolean rutinaTerminadaHoy;       // Bandera para felicitar al usuario si ya entrenó

    /**
     * SUBCLASE: ASISTENCIA SIMPLE
     * Estructura ultraligera para mapear el historial de visitas del cliente.
     */
    public static class AsistenciaSimple implements Serializable {
        public String fecha;
        public String hora;
        public String hora_salida; // Permite calcular el tiempo de estadía en el gimnasio

        public AsistenciaSimple(String f, String h, String hs) {
            this.fecha = f;
            this.hora = h;
            this.hora_salida = hs;
        }
    }

    /**
     * SUBCLASE: EJERCICIO SIMPLE
     * Contenedor plano para inyectar la lista de ejercicios de la rutina en el DOM (Kiosko/Dashboard).
     */
    public static class EjercicioSimple implements Serializable {
        public String nombre;
        public String seriesReps; // Concatena el esfuerzo (ej. "4 Series x 12 Reps")

        public EjercicioSimple(String n, String s) {
            this.nombre = n;
            this.seriesReps = s;
        }
    }
}