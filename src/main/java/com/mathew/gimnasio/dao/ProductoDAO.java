/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DE PRODUCTOS
 * Componente de acceso a datos encargado de gestionar el catálogo de productos de la tienda física y virtual.
 * Implementa un diseño de consulta optimizado que separa la recuperación de la información básica
 * (metadatos) de la extracción de recursos binarios pesados (imágenes) para garantizar un rendimiento eficiente.
 */
public class ProductoDAO {

    /**
     * LISTAR PRODUCTOS (VERSIÓN LIGERA)
     * Recupera el listado completo de productos disponibles para una sucursal específica, excluyendo
     * deliberadamente los datos binarios (imágenes) para prevenir la saturación de memoria y optimizar el tiempo de respuesta.
     * Parametro idEmpresa: Identificador de la sucursal o franquicia actual.
     * Retorna: Una lista de objetos Producto poblados con su información descriptiva y de costos.
     */
    public List<Producto> listarProductos(int idEmpresa) {
        List<Producto> lista = new ArrayList<>();

        // Consulta optimizada para la recuperación exclusiva de metadatos
        String sql = "SELECT id_producto, nombre, descripcion, precio, tipo FROM productos WHERE id_empresa = ? ORDER BY id_producto ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpresa); // Inyección segura del identificador de la empresa

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setIdProducto(rs.getInt("id_producto"));
                    p.setNombre(rs.getString("nombre"));
                    p.setDescripcion(rs.getString("descripcion"));
                    p.setPrecio(rs.getDouble("precio"));
                    p.setTipo(rs.getString("tipo"));
                    lista.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    /**
     * OBTENER IMAGEN DEL PRODUCTO (BINARIO)
     * Ejecuta una consulta directa a la columna de tipo BYTEA en la base de datos para extraer
     * el flujo binario correspondiente a la fotografía de un producto específico.
     * Parametro id: Identificador único del producto en el catálogo.
     * Retorna: Un arreglo de bytes que representa la imagen, o nulo si el recurso no existe o falla la extracción.
     */
    public byte[] obtenerImagen(int id) {
        // Extracción aislada del recurso binario para renderizado bajo demanda
        String sql = "SELECT imagen FROM productos WHERE id_producto = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Transforma el flujo de datos SQL a un arreglo de bytes estándar
                return rs.getBytes("imagen");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}