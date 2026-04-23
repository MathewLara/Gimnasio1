package com.mathew.gimnasio.modelos;

import java.io.Serializable;

public class AsignarAlumnoDTO implements Serializable {
    public int idCliente;
    public Integer idRutinaAsignada; // Puede ser null si se elige "Ninguna"
    public String notas;
}
