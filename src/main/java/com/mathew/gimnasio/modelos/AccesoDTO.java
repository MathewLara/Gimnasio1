package com.mathew.gimnasio.modelos;

public class AccesoDTO {
    private String usuario;
    private String rol;
    private String horaIngreso; // Renombramos para mayor claridad
    private String horaSalida;  // <--- NUEVA VARIABLE
    private String ip;
    private String estado;

    // --- Getters y Setters Actualizados ---
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