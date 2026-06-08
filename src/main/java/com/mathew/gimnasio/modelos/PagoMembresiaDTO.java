/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

/**
 * DTO DE PAGOS Y MEMBRESÍAS
 * Objeto transaccional utilizado para capturar la intención de compra del cliente
 * desde el módulo de Checkout. Encapsula los datos financieros básicos para su
 * validación y persistencia en la capa DAO.
 */
public class PagoMembresiaDTO {
    private int idUsuario;
    private int idMembresia;
    private double monto;
    private int dias;

    // --- Getters y Setters ---

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdMembresia() { return idMembresia; }
    public void setIdMembresia(int idMembresia) { this.idMembresia = idMembresia; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public int getDias() { return dias; }
    public void setDias(int dias) { this.dias = dias; }
}