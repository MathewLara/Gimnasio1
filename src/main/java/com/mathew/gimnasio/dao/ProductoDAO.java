package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DEL PRODUCTO
 * Esta clase es la encargada de gestionar los productos de la tienda (suplementos, ropa, etc.).
 * Su diseño es especial porque separa la información básica de las imágenes pesadas
 * para que la tienda cargue de forma instantánea.
 */
public class ProductoDAO {

    /**
     * LISTAR PRODUCTOS (VERSION LIGERA)
     * Trae todos los detalles de los productos (nombre, precio, descripción) pero NO la foto.
     * Esto se hace así para que la base de datos responda rápido y no sature la memoria.
     * @return Una lista de objetos Producto con su información básica.
     */
    public List<Producto> listarProductos() {
        List<Producto> lista = new ArrayList<>();
        // Seleccionamos solo las columnas de texto y números
        String sql = "SELECT id_producto, nombre, descripcion, precio, tipo, imagen_url FROM productos ORDER BY id_producto ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Producto p = new Producto();
                p.setIdProducto(rs.getInt("id_producto"));
                p.setNombre(rs.getString("nombre"));
                p.setDescripcion(rs.getString("descripcion"));
                p.setPrecio(rs.getDouble("precio"));
                p.setTipo(rs.getString("tipo"));
                p.setImagenUrl(rs.getString("imagen_url"));
                lista.add(p);
            }
        } catch (Exception e)
        // Si hay un error en la consulta, lo mostramos en la consola del servidor
        { e.printStackTrace(); }

        return lista;
    }

    /**
     * OBTENER IMAGEN (BYTES)
     * Este metodo se llama solo cuando la página web necesita mostrar la foto de un producto.
     * Va a la base de datos y extrae los datos binarios (la foto) del producto solicitado.
     * @param id El número de identificación del producto.
     * @return Un arreglo de bytes (la imagen) o null si no tiene foto.
     */
    public byte[] obtenerImagen(int id) {
        // Buscamos específicamente la columna 'imagen' que es de tipo BYTEA en PostgreSQL
        String sql = "SELECT imagen FROM productos WHERE id_producto = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Retornamos los bytes de la imagen para que el controlador los convierta en foto
                return rs.getBytes("imagen");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null; // Si algo falla o no hay imagen, devolvemos vacío
    }

    /**
     * CREAR PRODUCTO
     * Inserta un nuevo producto en la base de datos de la tienda.
     */
    public boolean crearProducto(Producto p) {
        String sql = "INSERT INTO productos (nombre, descripcion, precio, tipo, imagen_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecio());
            ps.setString(4, p.getTipo());
            ps.setString(5, p.getImagenUrl());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ACTUALIZAR PRODUCTO
     * Modifica los datos de un producto existente.
     */
    public boolean actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, tipo = ?, imagen_url = ? WHERE id_producto = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecio());
            ps.setString(4, p.getTipo());
            ps.setString(5, p.getImagenUrl());
            ps.setInt(6, p.getIdProducto());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ELIMINAR PRODUCTO
     * Borra un producto del catálogo definitivamente.
     */
    public boolean eliminarProducto(int idProducto) {
        String sql = "DELETE FROM productos WHERE id_producto = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idProducto);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}