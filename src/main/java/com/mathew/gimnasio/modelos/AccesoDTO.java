package com.mathew.gimnasio.modelos;

public class AccesoDTO {
    private String usuario;
    private String rol;
    private String hora;
    private String ip;
    private String estado;

    // --- Getters y Setters ---
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}