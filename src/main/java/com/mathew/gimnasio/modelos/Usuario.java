/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * ENTIDAD CORE: USUARIO
 * A diferencia de los DTOs, esta es una Entidad de Persistencia directa.
 * Mapea la estructura central de la base de datos que maneja la identidad,
 * credenciales y datos personales básicos de cualquier actor del sistema.
 */
public class Usuario implements Serializable {

    // --- Credenciales y Control de Acceso ---
    private int idUsuario;           // Primary Key (Serial)
    private int idRol;               // Foreign Key hacia la tabla roles (Admin, Recep, etc.)
    private int idEmpresa;
    private String usuario;          // Username único para login
    private String contrasena;       // Almacena el Hash BCrypt (NUNCA en texto plano)
    private Timestamp fechaCreacion; // Auditoría de creación
    private boolean activo;          // Bandera de borrado lógico (Soft Delete)
    private String email;            // Correo de contacto y recuperación

    // --- Datos Personales (Perfil Completo) ---
    private String nombre;
    private String apellido;
    private String telefono;
    private String cedula;           // Documento de identidad (DNI/Cédula)
    private String fechaNacimiento;  // Formato YYYY-MM-DD

    public Usuario() {} // Constructor vacío necesario para instancias dinámicas

    // --- Getters y Setters ---

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public int getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(int idEmpresa) { this.idEmpresa = idEmpresa; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
}