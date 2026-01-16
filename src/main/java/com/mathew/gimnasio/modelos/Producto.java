package com.mathew.gimnasio.modelos;

import java.io.Serializable;

public class Producto implements Serializable {
    private int idProducto;
    private String nombre;
    private String descripcion;
    private double precio;
    private String tipo;
    // No ponemos la imagen aquí para que la lista cargue rápido

    public Producto() {}

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}