/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO DEL DASHBOARD DEL ENTRENADOR
 * Estructura de datos jerárquica que centraliza la telemetría operativa del entrenador,
 * incluyendo agenda, biblioteca de rutinas y cartera de alumnos, permitiendo una
 * carga eficiente de la interfaz mediante un único contrato de transferencia.
 */
public class EntrenadorDashboardDTO implements Serializable {

    public String nombre;
    public String especialidad;
    public int rutinasCreadas;
    public int totalAlumnos;

    public List<RutinaItem> listaRutinas;
    public List<AlumnoResumen> listaAlumnos;

    /**
     * SUBCLASE: RESUMEN DE ALUMNO
     * Representación plana optimizada para la renderización de tablas de seguimiento
     * de alumnos sin necesidad de instanciar entidades completas.
     */
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

    /**
     * SUBCLASE: ITEM DE RUTINA
     * Define los atributos necesarios para el control y gestión de la biblioteca personal
     * de rutinas, incluyendo indicadores de estado lógico.
     */
    public static class RutinaItem implements Serializable {
        public int id;
        public String nombre;
        public boolean activa;
        public int idCliente;
        public List<Integer> idsEjercicios;

        public RutinaItem(int id, String nombre, boolean activa, int idCliente) {
            this.id = id;
            this.nombre = nombre;
            this.activa = activa;
            this.idCliente = idCliente;
            this.idsEjercicios = new java.util.ArrayList<>();
        }
    }
}