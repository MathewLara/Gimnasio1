package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

public class NuevaRutinaDTO implements Serializable {
    private int idCliente;
    private String nombreRutina;
    private List<Integer> idsEjercicios;

    public NuevaRutinaDTO() {}

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombreRutina() { return nombreRutina; }
    public void setNombreRutina(String nombreRutina) { this.nombreRutina = nombreRutina; }

    public List<Integer> getIdsEjercicios() { return idsEjercicios; }
    public void setIdsEjercicios(List<Integer> idsEjercicios) { this.idsEjercicios = idsEjercicios; }
}