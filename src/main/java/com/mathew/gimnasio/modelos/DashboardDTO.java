package com.mathew.gimnasio.modelos;

import java.util.List;

/**
 * CLASE DTO DEL DASHBOARD ADMINISTRATIVO
 * Este objeto actúa como un contenedor global de métricas (KPIs).
 * En lugar de hacer 5 peticiones distintas al servidor para armar la pantalla del Admin,
 * el backend consolida todos los totales, ingresos y listas recientes en este único objeto,
 * optimizando enormemente el ancho de banda y el tiempo de carga de la SPA.
 */
public class DashboardDTO {
    private int totalCuentas;                   // Total de usuarios registrados en el sistema
    private double ingresos;                    // Suma total de ingresos financieros (caja)
    private int totalEntrenadores;              // Cantidad de entrenadores activos en la nómina
    private List<AccesoDTO> ultimosAccesos;     // Historial reciente de escaneos en puerta (entradas/salidas)

    // Nueva variable para membresías vencidas
    private int membresiasVencidas;             // Indicador de alerta para clientes con pagos atrasados

    // --- Getters y Setters ---

    public int getTotalCuentas() { return totalCuentas; }
    public void setTotalCuentas(int totalCuentas) { this.totalCuentas = totalCuentas; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }

    public int getTotalEntrenadores() { return totalEntrenadores; }
    public void setTotalEntrenadores(int totalEntrenadores) { this.totalEntrenadores = totalEntrenadores; }

    public List<AccesoDTO> getUltimosAccesos() { return ultimosAccesos; }
    public void setUltimosAccesos(List<AccesoDTO> ultimosAccesos) { this.ultimosAccesos = ultimosAccesos; }

    // Nuevos métodos para gestionar las alertas de membresías vencidas
    public int getMembresiasVencidas() { return membresiasVencidas; }
    public void setMembresiasVencidas(int membresiasVencidas) { this.membresiasVencidas = membresiasVencidas; }
}