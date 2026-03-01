-- Migración para endpoints RF (Gimnasio API)
-- Ejecutar en PostgreSQL si las columnas/tablas no existen

-- 1. id_cliente en pagos (para historial de pagos por cliente)
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS id_cliente INTEGER REFERENCES clientes(id_cliente);

-- 2. id_cliente en factura_encabezados (para vincular ventas/comprobantes a clientes)
ALTER TABLE factura_encabezados ADD COLUMN IF NOT EXISTS id_cliente INTEGER REFERENCES clientes(id_cliente);

-- 3. descanso en detalle_rutinas (RF05 - series, reps, descanso)
ALTER TABLE detalle_rutinas ADD COLUMN IF NOT EXISTS descanso VARCHAR(50);
