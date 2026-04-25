package com.mathew.gimnasio.modelos;

/**
 * CLASE DTO DE PAGOS Y MEMBRESÍAS
 * Objeto transaccional utilizado para capturar la intención de compra del cliente
 * desde el módulo de Checkout o Punto de Venta. Encapsula los datos financieros
 * básicos antes de enviarlos al VentaDAO para su validación y persistencia.
 */
public class PagoMembresiaDTO {
    private int idUsuario;     // ID del usuario que está realizando la compra
    private int idMembresia;   // ID del plan seleccionado (ej. 1=Diario, 2=Mensual)
    private double monto;      // Valor monetario a cobrar (Validado en backend para evitar fraudes en frontend)
    private int dias;          // Cantidad de días de vigencia que otorga este pago

    // --- Getters y Setters ---
    // Métodos de acceso encapsulado requeridos para la correcta serialización JSON

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdMembresia() { return idMembresia; }
    public void setIdMembresia(int idMembresia) { this.idMembresia = idMembresia; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public int getDias() { return dias; }
    public void setDias(int dias) { this.dias = dias; }
}