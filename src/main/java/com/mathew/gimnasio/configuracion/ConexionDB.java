package com.mathew.gimnasio.configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    // 1. URL para PostgreSQL (El puerto estándar es 5432)
    // "gimnasio" es el nombre de tu base de datos (según veo en tu panel derecho)
    private static final String URL = "jdbc:postgresql://localhost:5432/gimnasio";

    // 2. Tus credenciales de PgAdmin
    private static final String USER = "postgres";
    private static final String PASS = "161825"; // <--- ¡CAMBIA ESTO!

    static {
        try {
            // Cargamos el driver de PostgreSQL
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el Driver de PostgreSQL.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
