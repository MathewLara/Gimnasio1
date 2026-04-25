package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * ENTIDAD/DTO DE PRODUCTO (Catálogo E-commerce)
 * Representa un artículo físico o servicio de la tienda del gimnasio (ej. Suplementos, Aguas).
 * Implementa Serializable para permitir su transferencia a través de la red de forma óptima.
 */
public class Producto implements Serializable {
    private int idProducto;       // Identificador único en el catálogo
    private String nombre;        // Nombre comercial del producto
    private String descripcion;   // Detalles y especificaciones
    private double precio;        // Valor unitario de venta
    private String tipo;          // Categoría (ej. Ropa, Suplemento, Bebida)

    /* * NOTA ARQUITECTÓNICA:
     * No incluimos la imagen (byte[] o Base64) en este DTO maestro.
     * Esto evita saturar la memoria del servidor y del cliente cuando se carga
     * una lista con cientos de productos. Las imágenes se manejan por separado (Lazy Loading).
     */

    public Producto() {} // Constructor requerido por JAX-RS (JSON Binding)

    // --- Getters y Setters ---

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