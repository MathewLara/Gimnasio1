/**
 * Autor: Mathew Lara
 * Fecha: 08/06/2026
 */
package com.mathew.gimnasio.dao;

import com.mathew.gimnasio.configuracion.ConexionDB;
import com.mathew.gimnasio.modelos.SolicitudVenta;
import java.sql.*;

/**
 * DAO DE VENTAS
 * Objeto de acceso a datos especializado en el procesamiento de transacciones comerciales.
 * Implementa transacciones atómicas (ACID) para garantizar la integridad financiera e inventarial
 * al operar sincronizadamente sobre pagos, facturas y los lotes de detalles.
 */
public class VentaDAO {

    /**
     * REGISTRAR VENTA COMPLETA (TIENDA DIGITAL)
     * Ejecuta una transacción relacional completa para consolidar una compra en el sistema.
     * Registra el pago en efectivo, genera la cabecera de facturación e inserta el lote
     * (batch operation) de productos adquiridos.
     * Parametro venta: Objeto estructural de transferencia que consolida el carrito y cliente.
     * Retorna: Verdadero si la transacción finaliza mediante commit, falso si ocurre un rollback general.
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

            // Inicia la transacción atómica desactivando la inserción automática
            conn.setAutoCommit(false);

            // 1. Registro financiero en el libro de pagos
            String sqlPago = "INSERT INTO pagos (monto_pagado, metodo_pago, fecha_pago) VALUES (?, 'EFECTIVO', NOW()) RETURNING id_pago";
            psPago = conn.prepareStatement(sqlPago);
            psPago.setDouble(1, venta.getTotal());
            rsPago = psPago.executeQuery();

            int idPago = 0;
            if (rsPago.next()) idPago = rsPago.getInt(1);

            // 2. Consolidación de cabecera de facturación
            String numFactura = "FAC-" + System.currentTimeMillis();
            String sqlFactura = "INSERT INTO factura_encabezados (id_pago, numero_factura, subtotal, iva, total_pagado, fecha_emision, id_usuario) VALUES (?, ?, ?, 0, ?, NOW(), ?)";
            psFactura = conn.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS);
            psFactura.setInt(1, idPago);
            psFactura.setString(2, numFactura);
            psFactura.setDouble(3, venta.getTotal());
            psFactura.setDouble(4, venta.getTotal());
            psFactura.setInt(5, venta.getIdUsuario());
            psFactura.executeUpdate();

            rsFactura = psFactura.getGeneratedKeys();
            int idFactura = 0;
            if (rsFactura.next()) idFactura = rsFactura.getInt(1);

            // 3. Ejecución por lotes (Batch Insert) para optimizar la inserción de los detalles
            String sqlDetalle = "INSERT INTO factura_detalles (id_factura, descripcion, cantidad, precio_unitario, subtotal_linea) VALUES (?, ?, ?, ?, ?)";
            psDetalle = conn.prepareStatement(sqlDetalle);

            for (SolicitudVenta.DetalleVenta item : venta.getProductos()) {
                psDetalle.setInt(1, idFactura);
                psDetalle.setString(2, item.getNombre());
                psDetalle.setInt(3, item.getCantidad());
                psDetalle.setDouble(4, item.getPrecio());
                psDetalle.setDouble(5, item.getPrecio() * item.getCantidad());
                psDetalle.addBatch();
            }
            psDetalle.executeBatch();

            // 4. Confirmación definitiva del conjunto de la transacción
            conn.commit();
            return true;

        } catch (Exception e) {
            // Revocación de la transacción en caso de excepción para mantener integridad de datos (Rollback)
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            // Liberación de recursos de conexión y memoria residual
            try { if (rsPago != null) rsPago.close(); } catch (Exception e) {}
            try { if (psPago != null) psPago.close(); } catch (Exception e) {}
            try { if (psDetalle != null) psDetalle.close(); } catch (Exception e) {}
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    // ==========================================
    // PAGO DE MEMBRESÍAS
    // ==========================================

    /**
     * REGISTRAR PAGO DE MEMBRESÍA (EN REVISIÓN PENDIENTE)
     * Almacena la evidencia digital (transferencia/depósito) asociada a la renovación
     * o adquisición de una membresía, catalogando el pago transaccionalmente
     * en estado 'PENDIENTE' para auditoría manual de recepción.
     * Parametro idUsuario: Identificador de la cuenta solicitante.
     * Parametro idMembresia: Identificador del plan seleccionado.
     * Parametro monto: Valor monetario declarado en el pago.
     * Parametro comprobanteFoto: Evidencia digital serializada en Base64.
     * Parametro numeroReferencia: Identificador del documento bancario.
     * Parametro motivo: Descripción contextual del pago (ej. Renovación).
     * Parametro idEmpresa: Sucursal a la cual se acredita el ingreso.
     * Retorna: Estado representativo de la transacción ("OK" o "Error detallado").
     */
    public String registrarPagoMembresia(int idUsuario, int idMembresia, double monto, String comprobanteFoto, String numeroReferencia, String motivo, int idEmpresa) {
        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();

            int idCliente = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id_cliente FROM clientes WHERE id_usuario = ?")) {
                ps.setInt(1, idUsuario);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) idCliente = rs.getInt("id_cliente");
                else return "Error: El usuario logueado no tiene perfil de cliente.";
            }

            // Inserción transaccional ajustando la zona horaria del servidor
            String sqlInsert = "INSERT INTO pagos (id_cliente, id_membresia, monto_pagado, metodo_pago, fecha_pago, estado, referencia_comprobante, foto_comprobante, motivo, id_empresa) " +
                    "VALUES (?, ?, ?, 'TRANSFERENCIA', CURRENT_TIMESTAMP - INTERVAL '5 hours', 'PENDIENTE', ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setInt(1, idCliente);
                ps.setInt(2, idMembresia);
                ps.setDouble(3, monto);
                ps.setString(4, numeroReferencia);
                ps.setString(5, comprobanteFoto);
                ps.setString(6, motivo);
                ps.setInt(7, idEmpresa);
                ps.executeUpdate();
            }

            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error BD: " + e.getMessage();
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }

    // ==========================================
    // GESTIÓN DE PEDIDOS PARA EL ADMINISTRADOR
    // ==========================================

    /**
     * OBTENER VENTAS COMERCIALES
     * Recupera la lista de transacciones correspondientes al módulo de tienda
     * evaluando su estado de despacho físico. Extrae las coincidencias implementando
     * aislamiento total de sucursal (Multi-Tenant).
     * Parametro idEmpresa: Identificador de la sucursal de negocio.
     * Retorna: Colección DTO con los detalles operacionales y el estado de entrega actual de la factura.
     */
    public java.util.List<com.mathew.gimnasio.modelos.VentaPendienteDTO> obtenerVentasPendientes(int idEmpresa) {
        java.util.List<com.mathew.gimnasio.modelos.VentaPendienteDTO> lista = new java.util.ArrayList<>();

        String sql = "SELECT f.id_factura, f.numero_factura, f.total_pagado, f.fecha_emision, f.estado_entrega, " +
                "COALESCE(c.nombre, 'Cliente') || ' ' || COALESCE(c.apellido, 'Web') as nombre_cliente " +
                "FROM factura_encabezados f " +
                "LEFT JOIN clientes c ON f.id_usuario = c.id_usuario " +
                "WHERE f.id_empresa = ? " +
                "ORDER BY f.fecha_emision DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpresa);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.mathew.gimnasio.modelos.VentaPendienteDTO v = new com.mathew.gimnasio.modelos.VentaPendienteDTO();
                    v.setIdFactura(rs.getInt("id_factura"));
                    v.setNumeroFactura(rs.getString("numero_factura"));
                    v.setTotalPagado(rs.getDouble("total_pagado"));
                    v.setFechaEmision(rs.getString("fecha_emision"));
                    v.setNombreCliente(rs.getString("nombre_cliente"));
                    v.setEstadoEntrega(rs.getString("estado_entrega"));
                    lista.add(v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * ACTUALIZAR ESTADO DE ENTREGA LOGÍSTICA
     * Modifica el indicador de la base de datos de un pedido físico de la tienda virtual,
     * notificando el cumplimiento y despacho de los productos pagados hacia las manos del cliente.
     * Parametro idFactura: Identificador correlativo del pedido comercial.
     * Retorna: Verdadero tras confirmar la alteración lógica en el registro principal de la factura.
     */
    public boolean marcarComoEntregado(int idFactura) {
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

    // ==========================================
    // OBTENER DETALLES PARA IMPRIMIR FACTURA
    // ==========================================

    /**
     * OBTENER DETALLE COMERCIAL DE FACTURA
     * Extrae de forma aislada la segmentación detallada de ítems (productos, cantidades, precios base)
     * asociados a un encabezado de cobro. Ideal para la renderización visual de comprobantes fiscales.
     * Parametro idFactura: Identificador único de la transacción analizada.
     * Retorna: Lista de elementos serializados de clave-valor que conforman los bienes facturados.
     */
    public java.util.List<java.util.Map<String, Object>> obtenerDetallesFactura(int idFactura) {
        java.util.List<java.util.Map<String, Object>> detalles = new java.util.ArrayList<>();
        String sql = "SELECT descripcion, cantidad, precio_unitario, subtotal_linea FROM factura_detalles WHERE id_factura = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idFactura);
            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("descripcion", rs.getString("descripcion"));
                item.put("cantidad", rs.getInt("cantidad"));
                item.put("precioUnitario", rs.getDouble("precio_unitario"));
                item.put("subtotalLinea", rs.getDouble("subtotal_linea"));
                detalles.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return detalles;
    }
}