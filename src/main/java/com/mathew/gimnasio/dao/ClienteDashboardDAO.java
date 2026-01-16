package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.ResumenClienteDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class ClienteDashboardDAO {
    public ResumenClienteDTO obtenerInfoDashboard(int idUsuario) {
        ResumenClienteDTO dto = new ResumenClienteDTO();
        try (Connection conn = ConexionDB.getConnection()) {

            // 1. PERFIL
            String sql = "SELECT id_cliente, nombre || ' ' || apellido as n, email, telefono FROM clientes WHERE id_usuario = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            int idCliente = 0;
            if (rs.next()) {
                idCliente = rs.getInt("id_cliente");
                dto.nombreCompleto = rs.getString("n");
                dto.email = rs.getString("email");
                dto.telefono = rs.getString("telefono");
            } else return null;

            // 2. ASISTENCIAS
            dto.historialAsistencias = new ArrayList<>();
            ps = conn.prepareStatement("SELECT to_char(fecha_hora_ingreso, 'YYYY-MM-DD') as f, to_char(fecha_hora_ingreso, 'HH24:MI') as h FROM asistencias WHERE id_cliente = ? ORDER BY fecha_hora_ingreso DESC LIMIT 5");
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();
            while(rs.next()) dto.historialAsistencias.add(new ResumenClienteDTO.AsistenciaSimple(rs.getString("f"), rs.getString("h")));

            // 3. RUTINA
            dto.ejercicios = new ArrayList<>();
            ps = conn.prepareStatement("SELECT r.id_rutina, r.nombre_rutina, COALESCE(e.nombre, 'Staff') as ent FROM rutinas r LEFT JOIN entrenadores e ON r.id_entrenador = e.id_entrenador WHERE r.id_cliente = ? ORDER BY r.id_rutina DESC LIMIT 1");
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            if(rs.next()){
                dto.nombreRutina = rs.getString("nombre_rutina");
                dto.entrenador = rs.getString("ent");
                int idR = rs.getInt("id_rutina");
                ps = conn.prepareStatement("SELECT e.nombre_ejercicio, d.series || ' x ' || d.repeticiones as sr FROM detalle_rutinas d JOIN ejercicios e ON d.id_ejercicio = e.id_ejercicio WHERE d.id_rutina = ?");
                ps.setInt(1, idR);
                rs = ps.executeQuery();
                while(rs.next()) dto.ejercicios.add(new ResumenClienteDTO.EjercicioSimple(rs.getString("nombre_ejercicio"), rs.getString("sr")));
            } else {
                dto.nombreRutina = "Sin Rutina";
                dto.entrenador = "--";
            }
        } catch (Exception e) { e.printStackTrace(); }
        return dto;
    }
}