package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EntrenadorDashboardDTO implements Serializable {
    public String nombre;
    public String especialidad;
    public int totalAlumnos;
    public int rutinasCreadas;

    // Listas para las tablas
    public List<String> listaRutinas = new ArrayList<>();
    public List<AlumnoResumen> listaAlumnos = new ArrayList<>();

    // Clase interna para resumir datos del alumno
    public static class AlumnoResumen {
        public int idCliente; // <--- AGREGAMOS ESTO
        public String nombre;
        public String plan;
        public String rutina;
        public boolean terminoHoy;

        // Actualizamos el constructor para recibir el ID
        public AlumnoResumen(int id, String n, String p, String r, boolean t) {
            this.idCliente = id; // <--- LO GUARDAMOS
            this.nombre = n;
            this.plan = p;
            this.rutina = r;
            this.terminoHoy = t;
        }
    }
}