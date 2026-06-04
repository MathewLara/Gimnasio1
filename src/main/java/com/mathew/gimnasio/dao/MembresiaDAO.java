package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Membresia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MembresiaDAO {

    public List<Membresia> listarPorEmpresa(int idEmpresa) {
        List<Membresia> lista = new ArrayList<>();
        // Consulta SQL estricta usando tus columnas reales
        String sql = "SELECT id_membresia, nombre, precio, descripcion FROM membresias WHERE id_empresa = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpresa);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Membresia m = new Membresia();
                    m.setId(rs.getInt("id_membresia"));
                    m.setNombre(rs.getString("nombre"));
                    m.setPrecio(rs.getDouble("precio"));
                    m.setDescripcion(rs.getString("descripcion")); // Captura tu columna 'descripcion'
                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar membresias: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}