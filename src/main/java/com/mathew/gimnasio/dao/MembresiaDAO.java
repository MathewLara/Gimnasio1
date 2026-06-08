/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Membresia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DE MEMBRESÍAS
 * Objeto de Acceso a Datos focalizado en la extracción y persistencia de
 * los planes comerciales disponibles en el sistema para una sucursal determinada.
 */
public class MembresiaDAO {

    /**
     * LISTAR MEMBRESÍAS POR EMPRESA
     * Realiza una consulta estricta a la base de datos para recuperar la colección
     * de planes de afiliación que pertenecen exclusivamente al catálogo
     * de la empresa solicitada, garantizando el aislamiento de datos (Multi-Tenant).
     * Parametro idEmpresa: Identificador único de la franquicia o sucursal.
     * Retorna: Una lista de objetos tipo Membresia poblados con la información de la base de datos.
     */
    public List<Membresia> listarPorEmpresa(int idEmpresa) {
        List<Membresia> lista = new ArrayList<>();
        // Consulta SQL estricta usando las columnas reales de la base de datos
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
                    m.setDescripcion(rs.getString("descripcion")); // Captura la columna 'descripcion'
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