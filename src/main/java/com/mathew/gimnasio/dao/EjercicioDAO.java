package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.util.JsonUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para catálogo de ejercicios (RF05)
 */
public class EjercicioDAO {

    public static class EjercicioItem {
        public int id;
        public String nombre;
        public String grupoMuscular;

        public EjercicioItem(int id, String nombre, String grupoMuscular) {
            this.id = id;
            this.nombre = nombre;
            this.grupoMuscular = grupoMuscular;
        }
    }

    public List<EjercicioItem> listarEjercicios() {
        List<EjercicioItem> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT id_ejercicio, nombre_ejercicio, grupo_muscular FROM ejercicios ORDER BY nombre_ejercicio";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new EjercicioItem(
                        rs.getInt("id_ejercicio"),
                        rs.getString("nombre_ejercicio"),
                        rs.getString("grupo_muscular")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public String listarEjerciciosJSON() {
        StringBuilder json = new StringBuilder("[");
        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "SELECT id_ejercicio, nombre_ejercicio, grupo_muscular FROM ejercicios ORDER BY nombre_ejercicio";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                json.append("{\"idEjercicio\":").append(rs.getInt("id_ejercicio"))
                        .append(",\"nombre\":\"").append(JsonUtil.escape(rs.getString("nombre_ejercicio")))
                        .append("\",\"grupoMuscular\":\"").append(JsonUtil.escape(rs.getString("grupo_muscular")))
                        .append("\"}");
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    public boolean crearEjercicio(String nombre, String grupoMuscular) {
        if (nombre == null || nombre.trim().isEmpty())
            return false;

        try (Connection conn = ConexionDB.getConnection()) {
            String sql = "INSERT INTO ejercicios (nombre_ejercicio, grupo_muscular) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nombre.trim());
            ps.setString(2, grupoMuscular != null ? grupoMuscular.trim() : "General");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
