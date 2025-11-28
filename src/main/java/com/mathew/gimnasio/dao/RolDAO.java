package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RolDAO {
    public List<String> obtenerRoles() {
        List<String> lista = new ArrayList<>();
        // PostgreSQL usa comillas dobles si la tabla fue creada con mayúsculas,
        // pero normalmente funciona sin ellas si todo es minúscula.
        String sql = "SELECT nombre_rol FROM roles";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre_rol"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            lista.add("Error: " + e.getMessage());
        }
        return lista;
    }
}
