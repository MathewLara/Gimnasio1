package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.sql.Timestamp;

public class Usuario implements Serializable {
    private int idUsuario;
    private int idRol;
    private String usuario;
    private String contrasena;
    private Timestamp fechaCreacion;
    private boolean activo;
    private String email;

    // --- CAMPOS PARA EL FORMULARIO COMPLETO ---
    private String nombre;          // <--- Nombre (Ej: Alan)
    private String apellido;        // <--- NUEVO CAMPO (Ej: Olivo)
    private String telefono;
    private String cedula;
    private String fechaNacimiento;

    public Usuario() {}

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // --- NUEVOS GETTERS Y SETTERS ---
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; } // <--- Getter Apellido
    public void setApellido(String apellido) { this.apellido = apellido; } // <--- Setter Apellido

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
}