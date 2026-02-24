package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO DE ROLES
 * Esta clase se encarga de consultar los diferentes tipos de permisos
 * o roles que existen en el sistema (ej. Administrador, Entrenador, Cliente, Recepción).
 */
public class RolDAO {

    /**
     * OBTENER LISTA DE ROLES
     * Se conecta a la base de datos y extrae únicamente los nombres de los roles disponibles.
     * Es ideal para llenar menús desplegables (selects) dinámicos en los formularios del frontend.
     * @return Una lista de textos (Strings) con los nombres de los roles.
     */
    public List<String> obtenerRoles() {
        List<String> lista = new ArrayList<>();
        // PostgreSQL usa comillas dobles si la tabla fue creada con mayúsculas,
        // pero normalmente funciona sin ellas si todo es minúscula.
        String sql = "SELECT nombre_rol FROM roles";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Recorremos los resultados fila por fila y los añadimos a la lista
            while (rs.next()) {
                lista.add(rs.getString("nombre_rol"));
            }
        } catch (Exception e) {
            // En caso de error (como base de datos apagada), lo imprimimos en consola
            // y devolvemos el mensaje de error dentro de la misma lista visual.
            e.printStackTrace();
            lista.add("Error: " + e.getMessage());
        }
        return lista;
    }
}