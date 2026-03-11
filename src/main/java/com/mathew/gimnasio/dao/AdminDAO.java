package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.DashboardDTO;
import com.mathew.gimnasio.modelos.AccesoDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public DashboardDTO obtenerEstadisticas() {
        DashboardDTO dash = new DashboardDTO();
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();

            // 1. Contar Total de Clientes
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalCuentas(rs.getInt(1));
            }

            // 2. Sumar Ingresos
            try(PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setIngresos(rs.getDouble(1));
            }

            // 3. Contar Entrenadores
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM entrenadores");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalEntrenadores(rs.getInt(1));
            }

            // 4. Llenar la Tabla de Accesos al Sistema (Nombres exactos)
            List<AccesoDTO> accesos = new ArrayList<>();

            // Usamos las columnas exactas de tu tabla: fecha_hora_log, direccion_ip, exitoso
            String sqlAccesos = "SELECT u.usuario, u.id_rol, a.fecha_hora_log, a.direccion_ip, a.exitoso " +
                    "FROM logs_acceso a " +
                    "INNER JOIN usuarios u ON a.id_usuario = u.id_usuario " +
                    "ORDER BY a.fecha_hora_log DESC LIMIT 5";

            try(PreparedStatement ps = conn.prepareStatement(sqlAccesos);
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    AccesoDTO acc = new AccesoDTO();
                    acc.setUsuario(rs.getString("usuario"));

                    int idRol = rs.getInt("id_rol");
                    acc.setRol(idRol == 1 ? "Admin" : "Cliente");

                    acc.setHora(rs.getString("fecha_hora_log"));
                    acc.setIp(rs.getString("direccion_ip"));

                    // Convertir el boolean de tu BD a texto para que se pinte en la web
                    boolean esExitoso = rs.getBoolean("exitoso");
                    acc.setEstado(esExitoso ? "Exitoso" : "Fallido");

                    accesos.add(acc);
                }
            } catch (Exception e) {
                System.out.println("Error en la consulta de accesos: " + e.getMessage());
            }
            dash.setUltimosAccesos(accesos);

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try { if(conn != null) conn.close(); } catch(Exception e) {}
        }
        return dash;
    }
}
