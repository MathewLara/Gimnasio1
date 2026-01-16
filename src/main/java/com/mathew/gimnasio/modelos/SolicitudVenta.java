package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

public class SolicitudVenta implements Serializable {
    private int idUsuario; // Quién compra
    private double total;
    private List<DetalleVenta> productos;

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<DetalleVenta> getProductos() { return productos; }
    public void setProductos(List<DetalleVenta> productos) { this.productos = productos; }

    // Subclase interna para los items
    public static class DetalleVenta implements Serializable {
        private int id; // idProducto
        private String nombre;
        private double precio;
        private int cantidad;

        // Getters y Setters
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