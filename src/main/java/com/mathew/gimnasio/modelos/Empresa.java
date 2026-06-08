/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

/**
 * MODELO DE EMPRESA
 * Representa la entidad de negocio principal en el modelo Multi-Tenant del sistema.
 * Almacena los datos institucionales y de contacto necesarios para identificar
 * y gestionar de forma aislada a cada franquicia o gimnasio independiente.
 */
public class Empresa {
    private int id;
    private String nombre;
    private String ruc;
    private String telefono;
    private String correo;
    private String direccion;

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}