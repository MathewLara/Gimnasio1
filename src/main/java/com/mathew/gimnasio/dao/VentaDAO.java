package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import java.sql.*;

public class VentaDAO {

    public boolean registrarVenta(SolicitudVenta venta) {
        Connection conn = null;
        PreparedStatement psPago = null;
        PreparedStatement psFactura = null;
        PreparedStatement psDetalle = null;
        ResultSet rsPago = null;
        ResultSet rsFactura = null;

        try {
            conn = ConexionDB.getConnection();
            // IMPORTANTE: Desactivamos auto-commit para manejar transacción
            // Si falla algo, se deshace todo.
            conn.setAutoCommit(false);

            // 1. Insertar en tabla PAGOS
            String sqlPago = "INSERT INTO pagos (monto_pagado, metodo_pago, fecha_pago) VALUES (?, 'EFECTIVO', NOW()) RETURNING id_pago";
            psPago = conn.prepareStatement(sqlPago);
            psPago.setDouble(1, venta.getTotal());
            rsPago = psPago.executeQuery();

            int idPago = 0;
            if (rsPago.next()) idPago = rsPago.getInt(1);

            // 2. Insertar en FACTURA_ENCABEZADOS
            // Generamos un número de factura aleatorio o secuencial simple
            String numFactura = "FAC-" + System.currentTimeMillis();
            String sqlFactura = "INSERT INTO factura_encabezados (id_pago, numero_factura, subtotal, iva, total_pagado, fecha_emision) VALUES (?, ?, ?, 0, ?, NOW()) RETURNING id_factura";
            psFactura = conn.prepareStatement(sqlFactura);
            psFactura.setInt(1, idPago);
            psFactura.setString(2, numFactura);
            psFactura.setDouble(3, venta.getTotal());
            psFactura.setDouble(4, venta.getTotal());
            rsFactura = psFactura.executeQuery();

            int idFactura = 0;
            if (rsFactura.next()) idFactura = rsFactura.getInt(1);

            // 3. Insertar FACTURA_DETALLES (Uno por uno)
            String sqlDetalle = "INSERT INTO factura_detalles (id_factura, descripcion, cantidad, precio_unitario, subtotal_linea) VALUES (?, ?, ?, ?, ?)";
            psDetalle = conn.prepareStatement(sqlDetalle);

            for (SolicitudVenta.DetalleVenta item : venta.getProductos()) {
                psDetalle.setInt(1, idFactura);
                psDetalle.setString(2, item.getNombre()); // Guardamos el nombre del producto
                psDetalle.setInt(3, item.getCantidad());
                psDetalle.setDouble(4, item.getPrecio());
                psDetalle.setDouble(5, item.getPrecio() * item.getCantidad());
                psDetalle.addBatch(); // Agregamos al lote
            }
            psDetalle.executeBatch(); // Ejecutamos todos los insert de golpe

            // Si todo salió bien, confirmamos los cambios
            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            // Cerrar recursos manualmente o usar try-with-resources en versiones modernas
            try { if (rsPago != null) rsPago.close(); } catch (Exception e) {}
            try { if (psPago != null) psPago.close(); } catch (Exception e) {}
            try { if (psDetalle != null) psDetalle.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
}