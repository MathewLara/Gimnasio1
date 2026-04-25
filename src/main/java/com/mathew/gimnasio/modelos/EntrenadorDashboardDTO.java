package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * CLASE DTO DEL DASHBOARD DEL ENTRENADOR
 * Este DTO es complejo y jerárquico. Centraliza toda la telemetría,
 * agenda y biblioteca de rutinas que el entrenador necesita ver al iniciar sesión.
 */
public class EntrenadorDashboardDTO implements Serializable {

    // --- Datos Básicos del Entrenador ---
    public String nombre;
    public String especialidad;

    // --- Contadores de Rendimiento (KPIs) ---
    public int rutinasCreadas;
    public int totalAlumnos;

    // --- Biblioteca de Rutinas ---
    // Usamos una clase anidada para enviar solo los datos necesarios al Front
    public List<RutinaItem> listaRutinas;

    // --- Cartera de Alumnos ---
    public List<AlumnoResumen> listaAlumnos;

    /**
     * SUBCLASE: RESUMEN DE ALUMNO
     * Estructura plana para renderizar la tabla de "Mis Alumnos" en el Frontend
     * sin necesidad de cargar toda la entidad "Cliente" desde la base de datos.
     */
    public static class AlumnoResumen {
        public int idCliente;
        public String nombre;
        public String plan;
        public String rutina;
        public boolean terminoHoy; // Bandera booleana para marcar la asistencia en la agenda

        public AlumnoResumen(int id, String n, String p, String r, boolean t) {
            this.idCliente = id;
            this.nombre = n;
            this.plan = p;
            this.rutina = r;
            this.terminoHoy = t;
        }
    }

    /**
     * SUBCLASE: ITEM DE RUTINA
     * Sirve para listar la biblioteca de rutinas del entrenador.
     * Contiene banderas vitales para permitir su edición y clonación.
     */
    public static class RutinaItem implements Serializable {
        public int id;
        public String nombre;
        public boolean activa; // Bandera de borrado lógico (True = Activa, False = En Papelera)
        public int idCliente;  // Identificador del propietario (Si es 0, es una Plantilla base)
        public List<Integer> idsEjercicios; // Lista de IDs para marcar los checkboxes al editar

        public RutinaItem(int id, String nombre, boolean activa, int idCliente) {
            this.id = id;
            this.nombre = nombre;
            this.activa = activa;
            this.idCliente = idCliente;
            this.idsEjercicios = new java.util.ArrayList<>();
        }
    }
}