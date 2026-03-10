package com.mathew.gimnasio.modelos;
import java.util.List;

public class DashboardDTO {
    private int totalCuentas;
    private double ingresos;
    private int totalEntrenadores;
    private List<AccesoDTO> ultimosAccesos;

    // Getters y Setters
    public int getTotalCuentas() { return totalCuentas; }
    public void setTotalCuentas(int totalCuentas) { this.totalCuentas = totalCuentas; }
    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }
    public int getTotalEntrenadores() { return totalEntrenadores; }
    public void setTotalEntrenadores(int totalEntrenadores) { this.totalEntrenadores = totalEntrenadores; }
    public List<AccesoDTO> getUltimosAccesos() { return ultimosAccesos; }
    public void setUltimosAccesos(List<AccesoDTO> ultimosAccesos) { this.ultimosAccesos = ultimosAccesos; }
}