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

            // 2. Sumar Ingresos (Todo lo que has cobrado)
            try(PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setIngresos(rs.getDouble(1));
            }

            // 3. Contar Entrenadores
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM entrenadores");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) dash.setTotalEntrenadores(rs.getInt(1));
            }

            // 4. Llenar la Tabla de Actividad Reciente (Usaremos los últimos pagos como actividad)
            List<AccesoDTO> accesos = new ArrayList<>();
            String sqlPagos = "SELECT id_pago, monto_pagado, fecha_pago FROM pagos ORDER BY fecha_pago DESC LIMIT 5";
            try(PreparedStatement ps = conn.prepareStatement(sqlPagos);
                ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    AccesoDTO acc = new AccesoDTO();
                    acc.setUsuario("Suscripción #" + rs.getInt("id_pago"));
                    acc.setAccion("Ingreso de $" + rs.getDouble("monto_pagado"));
                    acc.setFecha(rs.getString("fecha_pago"));
                    accesos.add(acc);
                }
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
