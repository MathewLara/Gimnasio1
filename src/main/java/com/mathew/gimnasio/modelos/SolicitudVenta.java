/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

/**
 * DTO COMPUESTO: SOLICITUD DE VENTA (Carrito de Compras)
 * Este objeto captura el "Checkout" completo de la tienda virtual.
 * Representa una relación transaccional de Cabecera-Detalle (1:N), donde
 * un usuario realiza una orden que contiene múltiples productos.
 */
public class SolicitudVenta implements Serializable {
    private int idUsuario;               // ID del cliente que realiza la compra
    private double total;                // Monto total a facturar (validado en servidor)
    private List<DetalleVenta> productos; // Lista de artículos en el carrito

    public SolicitudVenta() {} // Constructor para serialización JSON

    // --- Getters y Setters de la Cabecera ---
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public List<DetalleVenta> getProductos() { return productos; }
    public void setProductos(List<DetalleVenta> productos) { this.productos = productos; }

    /**
     * SUBCLASE: DETALLE DE VENTA
     * Representa una línea individual dentro de la factura (Un producto y su cantidad).
     */
    public static class DetalleVenta implements Serializable {
        private int id;          // ID del Producto en el catálogo
        private String nombre;   // Nombre del artículo
        private double precio;   // Precio unitario al momento de la compra
        private int cantidad;    // Unidades solicitadas

        // --- Getters y Setters del Detalle ---
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public double getPrecio() { return precio; }
        public void setPrecio(double precio) { this.precio = precio; }

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    }
}