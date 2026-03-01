package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import com.mathew.gimnasio.util.JsonUtil;

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

            // Obtener id_cliente si existe (para historial y comprobantes)
            Integer idCliente = null;
            if (venta.getIdUsuario() > 0) {
                PreparedStatement psCliente = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?");
                psCliente.setInt(1, venta.getIdUsuario());
                ResultSet rsCliente = psCliente.executeQuery();
                if (rsCliente.next()) idCliente = rsCliente.getInt("id_cliente");
            }

            /* 2. REGISTRAR EL PAGO (con id_cliente si existe - ver migracion_rf.sql) */
            String sqlPago = idCliente != null
                    ? "INSERT INTO pagos (monto_pagado, metodo_pago, fecha_pago, id_cliente) VALUES (?, 'EFECTIVO', NOW(), ?) RETURNING id_pago"
                    : "INSERT INTO pagos (monto_pagado, metodo_pago, fecha_pago) VALUES (?, 'EFECTIVO', NOW()) RETURNING id_pago";
            psPago = conn.prepareStatement(sqlPago);
            psPago.setDouble(1, venta.getTotal());
            if (idCliente != null) psPago.setInt(2, idCliente);
            rsPago = psPago.executeQuery();

            int idPago = 0;
            if (rsPago.next()) idPago = rsPago.getInt(1);

            /* 3. CREAR EL ENCABEZADO DE LA FACTURA
             * Generamos un número de factura único usando el tiempo actual (FAC-timestamp).
             * Vinculamos esta factura con el ID del pago que acabamos de crear arriba.
             */
            String numFactura = "FAC-" + System.currentTimeMillis();
            String sqlFactura = idCliente != null
                    ? "INSERT INTO factura_encabezados (id_pago, numero_factura, subtotal, iva, total_pagado, fecha_emision, id_cliente) VALUES (?, ?, ?, 0, ?, NOW(), ?) RETURNING id_factura"
                    : "INSERT INTO factura_encabezados (id_pago, numero_factura, subtotal, iva, total_pagado, fecha_emision) VALUES (?, ?, ?, 0, ?, NOW()) RETURNING id_factura";
            psFactura = conn.prepareStatement(sqlFactura);
            psFactura.setInt(1, idPago);
            psFactura.setString(2, numFactura);
            psFactura.setDouble(3, venta.getTotal());
            psFactura.setDouble(4, venta.getTotal());
            if (idCliente != null) psFactura.setInt(5, idCliente);
            rsFactura = psFactura.executeQuery();

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
            try { if (rsFactura != null) rsFactura.close(); } catch (Exception e) {}
            try { if (psFactura != null) psFactura.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    /**
     * Obtiene el comprobante (factura + detalle) como JSON (RF07).
     */
    public String obtenerComprobanteJSON(int idFactura) {
        StringBuilder json = new StringBuilder();
        try (Connection conn = ConexionDB.getConnection()) {
            PreparedStatement psEnc = conn.prepareStatement(
                    "SELECT f.id_factura, f.numero_factura, f.fecha_emision, f.subtotal, f.iva, f.total_pagado " +
                    "FROM factura_encabezados f WHERE f.id_factura = ?");
            psEnc.setInt(1, idFactura);
            ResultSet rsEnc = psEnc.executeQuery();
            if (!rsEnc.next()) return null;

            json.append("{")
                    .append("\"idFactura\":").append(rsEnc.getInt("id_factura")).append(",")
                    .append("\"numeroFactura\":\"").append(rsEnc.getString("numero_factura")).append("\",")
                    .append("\"fechaEmision\":\"").append(rsEnc.getTimestamp("fecha_emision")).append("\",")
                    .append("\"subtotal\":").append(rsEnc.getDouble("subtotal")).append(",")
                    .append("\"iva\":").append(rsEnc.getDouble("iva")).append(",")
                    .append("\"totalPagado\":").append(rsEnc.getDouble("total_pagado")).append(",")
                    .append("\"detalle\":[");

            PreparedStatement psDet = conn.prepareStatement(
                    "SELECT descripcion, cantidad, precio_unitario, subtotal_linea FROM factura_detalles WHERE id_factura = ?");
            psDet.setInt(1, idFactura);
            ResultSet rsDet = psDet.executeQuery();
            boolean first = true;
            while (rsDet.next()) {
                if (!first) json.append(",");
                json.append("{\"descripcion\":\"").append(JsonUtil.escape(rsDet.getString("descripcion")))
                        .append("\",\"cantidad\":").append(rsDet.getInt("cantidad"))
                        .append(",\"precioUnitario\":").append(rsDet.getDouble("precio_unitario"))
                        .append(",\"subtotal\":").append(rsDet.getDouble("subtotal_linea")).append("}");
                first = false;
            }
            json.append("]}");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return json.toString();
    }
}