package com.mathew.gimnasio.modelos;

public class Membresia {
    private int id;
    private String nombre;
    private double precio;
    private String descripcion; // Cambiamos 'dias' por 'descripcion' según tu BD

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}