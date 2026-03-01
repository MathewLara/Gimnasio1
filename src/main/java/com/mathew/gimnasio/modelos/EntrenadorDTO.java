package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * DTO para CRUD de entrenadores (RF04)
 */
public class EntrenadorDTO implements Serializable {
    public String nombre;
    public String apellido;
    public String email;
    public String usuario;
    public String contrasena;
    public String especialidad;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
}
