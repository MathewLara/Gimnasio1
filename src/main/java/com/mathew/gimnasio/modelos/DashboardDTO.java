/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.util.List;

/**
 * DTO DEL DASHBOARD ADMINISTRATIVO
 * Objeto contenedor de métricas globales (KPIs). Centraliza diversos indicadores
 * de rendimiento financiero y operativo en un único contrato de datos, optimizando
 * el intercambio de información entre el servidor y el cliente (SPA).
 */
public class DashboardDTO {
    private int totalCuentas;
    private double ingresos;
    private int totalEntrenadores;
    private List<AccesoDTO> ultimosAccesos;
    private int membresiasVencidas;

    // --- Getters y Setters ---

    public int getTotalCuentas() { return totalCuentas; }
    public void setTotalCuentas(int totalCuentas) { this.totalCuentas = totalCuentas; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }

    public int getTotalEntrenadores() { return totalEntrenadores; }
    public void setTotalEntrenadores(int totalEntrenadores) { this.totalEntrenadores = totalEntrenadores; }

    public List<AccesoDTO> getUltimosAccesos() { return ultimosAccesos; }
    public void setUltimosAccesos(List<AccesoDTO> ultimosAccesos) { this.ultimosAccesos = ultimosAccesos; }

    public int getMembresiasVencidas() { return membresiasVencidas; }
    public void setMembresiasVencidas(int membresiasVencidas) { this.membresiasVencidas = membresiasVencidas; }
}