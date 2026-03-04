package com.mathew.gimnasio.modelos;

import java.io.Serializable;

/**
 * DTO para el endpoint POST /api/accesos/manual
 *
 * Permite al recepcionista registrar una entrada o salida manualmente
 * cuando el QR falla o el cliente no tiene teléfono.
 */
public class AccesoManualDTO implements Serializable {

    private int idCliente;       // ID de la tabla clientes (no id_usuario)
    private String tipo;         // "ENTRADA" o "SALIDA"
    private String motivo;       // Texto libre: "QR dañado", "Sin teléfono", etc.

    public AccesoManualDTO() {}

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}