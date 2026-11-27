package com.mathew.gimnasio.modelos;

public class SolicitudIaDTO {
    private double peso;
    private double altura;
    private int edad;
    private String meta;      // Ej: "Bajar de peso", "Ganar masa muscular"
    private String tipoPlan;  // Ej: "Rutina", "Alimentación", "Ambos"

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }
    public double getAltura() { return altura; }
    public void setAltura(double altura) { this.altura = altura; }
    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public String getTipoPlan() { return tipoPlan; }
    public void setTipoPlan(String tipoPlan) { this.tipoPlan = tipoPlan; }
}