/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DE ROLES
 * Objeto de acceso a datos responsable de consultar el catálogo de permisos y
 * roles de seguridad implementados en el sistema (ej. Administrador, Entrenador, Cliente).
 * Facilita la asignación de privilegios en la capa de presentación.
 */
public class RolDAO {

    /**
     * OBTENER LISTA DE ROLES
     * Establece una conexión de lectura con la base de datos para extraer los nombres
     * de los roles activos. Ideal para la carga dinámica de componentes UI como
     * listas desplegables (selects) en los formularios de administración.
     * Retorna: Una lista de cadenas de texto (Strings) correspondientes a los roles disponibles.
     */
    public List<String> obtenerRoles() {
        List<String> lista = new ArrayList<>();

        // Sentencia SQL orientada a la extracción exclusiva de la columna requerida
        String sql = "SELECT nombre_rol FROM roles";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Iteración sobre el conjunto de resultados para poblar la colección en memoria
            while (rs.next()) {
                lista.add(rs.getString("nombre_rol"));
            }
        } catch (Exception e) {
            // Manejo de excepciones de conexión o fallos en el motor de base de datos,
            // reportando el error en la salida estándar y notificándolo en la colección resultante
            e.printStackTrace();
            lista.add("Error: " + e.getMessage());
        }
        return lista;
    }
}