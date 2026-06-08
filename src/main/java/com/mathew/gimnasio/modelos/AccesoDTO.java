/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.modelos;

/**
 * DTO DE ACCESO
 * Objeto de Transferencia de Datos (Data Transfer Object) utilizado para empaquetar
 * y transportar la información relacionada con el registro de auditoría de accesos.
 * Facilita la comunicación entre la capa de acceso a datos y la capa de presentación
 * para el monitoreo de entradas y salidas del sistema o de las instalaciones físicas.
 */
public class AccesoDTO {
    private String usuario;
    private String rol;
    private String horaIngreso;
    private String horaSalida;
    private String ip;
    private String estado;

    // --- Getters y Setters ---
    // Proveen acceso seguro y controlado a los atributos encapsulados de la clase.

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