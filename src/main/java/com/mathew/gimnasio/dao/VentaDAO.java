package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import java.sql.*;

/**
 * DAO DE VENTA
 * Esta clase es la encargada de procesar las transacciones económicas de la tienda.
 * Es un proceso delicado porque debe afectar a tres tablas distintas (Pagos, Encabezados y Detalles)
 * de forma coordinada para que no haya errores de dinero o inventario.
 */
public class VentaDAO {

    /**
     * REGISTRAR UNA VENTA COMPLETA
     * Realiza un proceso de tres pasos dentro de una "Transacción".
     * @param venta Objeto que trae el total del carrito y la lista de productos.
     * @return true si la compra se guardó completa, false si hubo algún error y se canceló todo.
     */
    public boolean registrarVenta(SolicitudVenta venta) {
        Connection conn = null;
        PreparedStatement psPago = null;
        PreparedStatement psFactura = null;
        PreparedStatement psDetalle = null;
        ResultSet rsPago = null;
        ResultSet rsFactura = null;

        try {
            conn = ConexionDB.getConnection();
            /* 1. ACTIVAR MODO SEGURO (Transacción)
             * Desactivamos el 'auto-commit'. Esto significa que nada se guardará
             * definitivamente en PostgreSQL hasta que nosotros demos la orden final (commit).
             * Si algo falla en el camino, haremos un 'rollback' para borrar los pasos a medias.
             */
            conn.setAutoCommit(false);

            /* 2. REGISTRAR EL PAGO
             * Primero creamos el registro en la tabla de 'pagos'.
             * Usamos 'RETURNING id_pago' para saber qué número de pago generó el sistema.
             */
            String sqlPago = "INSERT INTO pagos (monto_pagado, metodo_pago, fecha_pago) VALUES (?, 'EFECTIVO', NOW()) RETURNING id_pago";
            psPago = conn.prepareStatement(sqlPago);
            psPago.setDouble(1, venta.getTotal());
            rsPago = psPago.executeQuery();

            int idPago = 0;
            if (rsPago.next()) idPago = rsPago.getInt(1);

            /* 3. CREAR EL ENCABEZADO DE LA FACTURA */
            String numFactura = "FAC-" + System.currentTimeMillis();
            String sqlFactura = "INSERT INTO factura_encabezados (id_pago, numero_factura, subtotal, iva, total_pagado, fecha_emision, id_usuario) VALUES (?, ?, ?, 0, ?, NOW(), ?)";
            psFactura = conn.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS);
            psFactura.setInt(1, idPago);
            psFactura.setString(2, numFactura);
            psFactura.setDouble(3, venta.getTotal());
            psFactura.setDouble(4, venta.getTotal());
            psFactura.setInt(5, venta.getIdUsuario()); // NUEVO: Guardamos el ID del cliente
            psFactura.executeUpdate();

            rsFactura = psFactura.getGeneratedKeys();
            int idFactura = 0;
            if (rsFactura.next()) idFactura = rsFactura.getInt(1);

            /* 4. REGISTRAR LOS PRODUCTOS (Detalles)
             * Recorremos la lista de productos que el cliente tenía en su carrito.
             * Usamos 'addBatch' para preparar todos los insert y 'executeBatch' para
             * enviarlos todos de un solo golpe, lo que hace que el sistema sea muy rápido.
             */
            String sqlDetalle = "INSERT INTO factura_detalles (id_factura, descripcion, cantidad, precio_unitario, subtotal_linea) VALUES (?, ?, ?, ?, ?)";
            psDetalle = conn.prepareStatement(sqlDetalle);

            for (SolicitudVenta.DetalleVenta item : venta.getProductos()) {
                psDetalle.setInt(1, idFactura);
                psDetalle.setString(2, item.getNombre()); // Guardamos el nombre del producto
                psDetalle.setInt(3, item.getCantidad());
                psDetalle.setDouble(4, item.getPrecio());
                psDetalle.setDouble(5, item.getPrecio() * item.getCantidad());
                psDetalle.addBatch(); // Agregamos este producto al "lote" de guardado
            }
            psDetalle.executeBatch(); // Guardamos todos los productos de la lista a la vez

            /* 5. CONFIRMACIÓN FINAL
             * Si llegamos aquí sin errores, le decimos a la base de datos que guarde todo
             * definitivamente con el comando 'commit'.
             */
            conn.commit();
            return true;

        } catch (Exception e) {
            /* * MANEJO DE EMERGENCIAS (Rollback)
             * Si algo falló (ej: se fue el internet o un dato estaba mal),
             * deshacemos absolutamente tod0 para no dejar una factura sin pago o un pago sin factura.
             */
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            /* * LIMPIEZA DE RECURSOS
             * Cerramos todas las conexiones y herramientas usadas para no saturar el servidor.
             */
            try { if (rsPago != null) rsPago.close(); } catch (Exception e) {}
            try { if (psPago != null) psPago.close(); } catch (Exception e) {}
            try { if (psDetalle != null) psDetalle.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    // ==========================================
    // PAGO DE MEMBRESÍAS
    // ==========================================
    public String registrarPagoMembresia(int idUsuario, int idMembresia, double monto, int dias) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Obtener el id_cliente
            int idCliente = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?")) {
                ps.setInt(1, idUsuario);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCliente = rs.getInt("id_cliente");
                else throw new SQLException("El usuario logueado no tiene un perfil en la tabla 'clientes'.");
            }

            // 2. Registrar el pago (¡CORREGIDO! Usamos id_membresia y metodo_pago como lo pide tu SQL)
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO pagos (id_membresia, monto_pagado, metodo_pago, fecha_pago) VALUES (?, ?, 'TARJETA WEB', CURRENT_TIMESTAMP)")) {
                ps.setInt(1, idMembresia);
                ps.setDouble(2, monto);
                ps.executeUpdate();
            }

            // 3. Actualizar la membresía sumando días automáticamente
            String sqlUpdate = "UPDATE clientes SET id_membresia = ?, fecha_vencimiento = COALESCE(fecha_vencimiento, CURRENT_DATE) + ? WHERE id_cliente = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, idMembresia);
                ps.setInt(2, dias); // Sumamos los días como un número entero normal
                ps.setInt(3, idCliente);
                ps.executeUpdate();
            }

            conn.commit();
            return "OK"; // Todo salió perfecto

        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            return "Error BD: " + e.getMessage();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }
    // ==========================================
    // GESTIÓN DE PEDIDOS PARA EL ADMINISTRADOR
    // ==========================================

    // 1. Obtener todas las facturas (Pendientes y Entregadas)
    public java.util.List<com.mathew.gimnasio.modelos.VentaPendienteDTO> obtenerVentasPendientes() {
        java.util.List<com.mathew.gimnasio.modelos.VentaPendienteDTO> lista = new java.util.ArrayList<>();

        // MODIFICACIÓN SQL:
        // - Usamos COALESCE para que si el apellido es nulo, no arruine el nombre.
        // - Traemos f.estado_entrega.
        // - Quitamos el WHERE y ponemos ORDER BY para ver los recientes primero.
        String sql = "SELECT f.id_factura, f.numero_factura, f.total_pagado, f.fecha_emision, f.estado_entrega, " +
                "COALESCE(u.nombre, 'Usuario') || ' ' || COALESCE(u.apellido, '') as nombre_cliente " +
                "FROM factura_encabezados f " +
                "LEFT JOIN usuarios u ON f.id_usuario = u.id_usuario " + // <-- Corregido u.id_usuario
                "ORDER BY f.fecha_emision DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                com.mathew.gimnasio.modelos.VentaPendienteDTO v = new com.mathew.gimnasio.modelos.VentaPendienteDTO();
                v.setIdFactura(rs.getInt("id_factura"));
                v.setNumeroFactura(rs.getString("numero_factura"));
                v.setTotalPagado(rs.getDouble("total_pagado"));
                v.setFechaEmision(rs.getString("fecha_emision"));
                v.setNombreCliente(rs.getString("nombre_cliente"));

                // NUEVO: Extraemos el estado de la base de datos y lo metemos en el DTO
                v.setEstadoEntrega(rs.getString("estado_entrega"));

                lista.add(v);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // 2. Cambiar el estado a ENTREGADO
    public boolean marcarComoEntregado(int idFactura) {
        // Actualizamos el estado en la base de datos
        String sql = "UPDATE factura_encabezados SET estado_entrega = 'ENTREGADO' WHERE id_factura = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idFactura);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}