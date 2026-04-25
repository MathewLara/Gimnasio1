package com.mathew.gimnasio.modelos;

/**
 * CLASE DTO DE ACCESO (Data Transfer Object)
 * Se utiliza para empaquetar y transferir la información relacionada con
 * el registro de accesos (entradas y salidas) de los usuarios al sistema o al gimnasio físico.
 */
public class AccesoDTO {
    private String usuario;     // Nombre de usuario que realiza el acceso
    private String rol;         // Rol del usuario en el sistema (ej. Administrador, Cliente)
    private String horaIngreso; // Hora exacta en la que el usuario ingresó (Renombramos para mayor claridad)
    private String horaSalida;  // Hora exacta en la que el usuario salió (<--- NUEVA VARIABLE)
    private String ip;          // Dirección IP desde donde se realizó la conexión o escaneo
    private String estado;      // Estado final del intento de acceso (ej. Exitoso, Denegado)

    // --- Getters y Setters Actualizados ---
    // Métodos públicos que permiten leer y modificar los atributos privados de forma segura

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getHoraIngreso() { return horaIngreso; }
    public void setHoraIngreso(String horaIngreso) { this.horaIngreso = horaIngreso; }

    public String getHoraSalida() { return horaSalida; }
    public void setHoraSalida(String horaSalida) { this.horaSalida = horaSalida; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}