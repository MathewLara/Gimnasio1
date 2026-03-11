package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;

public class RecepcionDAO {

    // ==========================================
    // 1. OBTENER ESTADÍSTICAS DEL DÍA (RESUMEN)
    // ==========================================
    public String getDashboardRecepJSON() {
        StringBuilder json = new StringBuilder("{");

        try (Connection conn = ConexionDB.getConnection()) {

            // 1. Total de clientes registrados (Para el primer cuadro)
            int totalClientes = 0;
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM clientes");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) totalClientes = rs.getInt(1);
            }

            // 2. Clientes activos (Membresía vigente)
            // *Asumimos que activo=true en usuarios para este ejemplo rápido*
            int clientesActivos = 0;
            try(PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM usuarios WHERE id_rol = 4 AND activo = TRUE");
                ResultSet rs = ps.executeQuery()) {
                if(rs.next()) clientesActivos = rs.getInt(1);
            }

            // 3. Accesos de HOY (Aforo actual)
            // *Ojo: si no tienes tabla de asistencia o accesos_clientes, mandaremos 0 por ahora*
            int accesosHoy = 0;

            // Armamos el JSON con los KPIs
            json.append("\"kpis\": {")
                    .append("\"totalClientes\": ").append(totalClientes).append(",")
                    .append("\"clientesActivos\": ").append(clientesActivos).append(",")
                    .append("\"accesosHoy\": ").append(accesosHoy)
                    .append("},");

            // 4. Actividad Reciente (Para la tabla)
            // Por ahora enviaremos la lista vacía hasta que hagamos el módulo de escáner QR
            json.append("\"actividadReciente\": []");

        } catch (Exception e) {
            System.out.println("Error en RecepcionDAO: " + e.getMessage());
            return "{\"kpis\": {\"totalClientes\": 0, \"clientesActivos\": 0, \"accesosHoy\": 0}, \"actividadReciente\": []}";
        }

        json.append("}");
        return json.toString();
    }
}
