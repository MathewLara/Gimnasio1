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

            /* 3. CREAR EL ENCABEZADO DE LA FACTURA
             * Generamos un número de factura único usando el tiempo actual (FAC-timestamp).
             * Vinculamos esta factura con el ID del pago que acabamos de crear arriba.
             */
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
    // PAGO DE MEMBRESÍAS (MOCK FINANCIERO)
    // ==========================================
    public boolean registrarPagoMembresia(int idUsuario, int idMembresia, double monto, int dias) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción segura

            System.out.println("INICIANDO PAGO: Usuario ID " + idUsuario + " | Plan ID " + idMembresia);

            // 1. Obtener el id_cliente asociado al id_usuario que está comprando
            int idCliente = -1;
            try (PreparedStatement psBusqueda = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?")) {
                psBusqueda.setInt(1, idUsuario);
                ResultSet rs = psBusqueda.executeQuery();
                if (rs.next()) {
                    idCliente = rs.getInt("id_cliente");
                } else {
                    System.out.println("ERROR: El usuario " + idUsuario + " no existe en la tabla 'clientes'.");
                    throw new SQLException("Cliente no encontrado.");
                }
            }

            // 2. Registrar el pago en la tabla pagos (El ingreso real para tu Dashboard)
            try (PreparedStatement psPago = conn.prepareStatement("INSERT INTO pagos (id_cliente, monto_pagado, fecha_pago) VALUES (?, ?, CURRENT_TIMESTAMP)")) {
                psPago.setInt(1, idCliente);
                psPago.setDouble(2, monto);
                psPago.executeUpdate();
            }

            // 3. Actualizar la membresía y sumarle los días de acceso
            // PostgreSQL suma días automáticamente si le sumas un número entero a una fecha (DATE)
            String sqlUpdate = "UPDATE clientes SET id_membresia = ?, fecha_vencimiento = COALESCE(fecha_vencimiento, CURRENT_DATE) + ? WHERE id_cliente = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                psUpdate.setInt(1, idMembresia);
                psUpdate.setInt(2, dias); // Se suma como un entero simple
                psUpdate.setInt(3, idCliente);
                psUpdate.executeUpdate();
            }

            conn.commit(); // Guardar todo definitivamente
            System.out.println("¡ÉXITO! Membresía renovada en la base de datos.");
            return true;

        } catch (Exception e) {
            System.out.println("ERROR CRÍTICO AL GUARDAR EN BASE DE DATOS:");
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {} // Deshacer si hay error
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }
}