package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

public class EntrenadorDashboardDTO implements Serializable {

    // Datos del Entrenador
    public String nombre;
    public String especialidad;

    // Contadores del Tablero
    public int rutinasCreadas;
    public int totalAlumnos;

    // --- CAMBIO IMPORTANTE AQUÍ ---
    // Antes era: public List<String> listaRutinas;
    // Ahora usamos una clase especial para llevar ID y Nombre:
    public List<RutinaItem> listaRutinas;

    // Lista de Alumnos
    public List<AlumnoResumen> listaAlumnos;

    // Clase interna para los Alumnos (Esta ya la tenías, se queda igual)
    public static class AlumnoResumen {
        public int idCliente;
        public String nombre;
        public String plan;
        public String rutina;
        public boolean terminoHoy;

        public AlumnoResumen(int id, String n, String p, String r, boolean t) {
            this.idCliente = id;
            this.nombre = n;
            this.plan = p;
            this.rutina = r;
            this.terminoHoy = t;
        }
    }

    // --- NUEVA CLASE ---
    // Esta clase sirve para guardar el ID de la rutina (para borrarla)
    // y el NOMBRE (para mostrarla en la lista).
    public static class RutinaItem implements Serializable {
        public int id;
        public String nombre;
        public boolean activa; // Nuevo: Para saber si es basura o no
        public int idCliente;  // Nuevo: Para saber de quién es al editar
        public List<Integer> idsEjercicios; // Nuevo: Para marcar los checks al editar

        public RutinaItem(int id, String nombre, boolean activa, int idCliente) {
            this.id = id;
            this.nombre = nombre;
            this.activa = activa;
            this.idCliente = idCliente;
            this.idsEjercicios = new java.util.ArrayList<>();
        }
    }
}