package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * Body para POST /membresias/{idCliente}
 */
public class MembresiaAsignacionDTO implements Serializable {
    public Integer idMembresia;   // id de tabla membresias
    public Integer idTipoMembresia; // alternativo: id de tipos_membresia (tiene duracion_dias)
    public Integer duracionDias;   // opcional, default 30

    public Integer getIdMembresia() { return idMembresia; }
    public void setIdMembresia(Integer idMembresia) { this.idMembresia = idMembresia; }
    public Integer getIdTipoMembresia() { return idTipoMembresia; }
    public void setIdTipoMembresia(Integer idTipoMembresia) { this.idTipoMembresia = idTipoMembresia; }
    public Integer getDuracionDias() { return duracionDias; }
    public void setDuracionDias(Integer duracionDias) { this.duracionDias = duracionDias; }
}
