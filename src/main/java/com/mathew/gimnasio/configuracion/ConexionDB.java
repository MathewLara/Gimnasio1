/**
 * Author: Mathew Lara
 * Fecha: 07/06/2026
 */
package com.mathew.gimnasio.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * CLASE DE CONFIGURACIÓN: CONEXIÓN A LA BASE DE DATOS
 * Esta clase actúa como el puente central de comunicación entre la aplicación Java y la base de datos PostgreSQL.
 * Implementa un enfoque de acceso estático para proveer instancias de conexión
 * seguras y directas cada vez que un componente DAO (Data Access Object) requiera realizar operaciones de persistencia.
 */
public class ConexionDB {

    // Constantes de conexión: Ruta, usuario y contraseña de la base de datos PostgreSQL alojada en Render
    private static final String URL = "jdbc:postgresql://dpg-d8gf2p3bc2fs73eghme0-a.ohio-postgres.render.com:5432/gimnasio_db_v4";
    private static final String USER = "gimnasio_db_v4_user";
    private static final String PASS = "pH4EZHqNNEJKwLuuS1WcdSwoPaQw5nhr";

    /**
     * METODO PRINCIPAL DE CONEXIÓN
     * Inicializa el controlador JDBC y establece una nueva conexión con el servidor de base de datos.
     * * Retorna:
     * Un objeto de tipo Connection listo para ejecutar sentencias SQL. Retorna null si ocurre un fallo crítico.
     */
    public static Connection getConnection() {
        try {
            // 1. Registro del driver JDBC de PostgreSQL en tiempo de ejecución
            Class.forName("org.postgresql.Driver");

            // 2. Creación y apertura de la conexión utilizando las credenciales establecidas
            return DriverManager.getConnection(URL, USER, PASS);

        } catch (ClassNotFoundException | SQLException e) {
            // Captura de excepciones críticas (Driver no encontrado o credenciales/URL inválidas)
            System.out.println("Error de conexión a la base de datos: " + e.getMessage());
            return null;
        }
    }
}