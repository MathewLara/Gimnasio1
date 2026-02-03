package com.mathew.gimnasio.modelos;

import java.io.Serializable;
import java.util.List;

public class NuevaRutinaDTO implements Serializable {
    public int idCliente;
    public String nombreRutina;
    public List<Integer> idsEjercicios; // Lista de IDs de ejercicios seleccionados
}
