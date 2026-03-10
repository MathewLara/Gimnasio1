package com.mathew.gimnasio.modelos;

/**
 * DTO (Data Transfer Object) para recibir los datos de pago desde el checkout.
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