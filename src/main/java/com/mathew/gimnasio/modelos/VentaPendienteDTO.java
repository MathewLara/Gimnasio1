/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * DTO DE LOGÍSTICA: VENTA PENDIENTE
 * Este objeto "aplana" la información de una factura para ser consumida
 * por el panel del Recepcionista o Administrador. Solo muestra lo necesario
 * para gestionar las entregas físicas de las compras web.
 */
public class VentaPendienteDTO implements Serializable {
    private int idFactura;         // ID interno de la base de datos
    private String numeroFactura;  // Código correlativo visual (ej. FAC-0001)
    private double totalPagado;    // Monto final
    private String fechaEmision;   // Fecha de la transacción
    private String estadoEntrega;  // Workflow logístico (PENDIENTE / ENTREGADO)
    private String nombreCliente;  // Nombre desnormalizado para evitar JOINs pesados en el Front

    // --- Getters y Setters ---

    public int getIdFactura() { return idFactura; }
    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(double totalPagado) { this.totalPagado = totalPagado; }

    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getEstadoEntrega() { return estadoEntrega; }
    public void setEstadoEntrega(String estadoEntrega) { this.estadoEntrega = estadoEntrega; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
}