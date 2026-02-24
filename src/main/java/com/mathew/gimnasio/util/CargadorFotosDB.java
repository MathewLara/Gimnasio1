package com.mathew.gimnasio.util;

import com.mathew.gimnasio.configuracion.ConexionDB;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class CargadorFotosDB {

    public static void main(String[] args) {
        String rutaCarpeta = "C:/Imagenes Gimnasio/";
        File carpeta = new File(rutaCarpeta);

        try (Connection conn = ConexionDB.getConnection()) {
            // Buscamos productos del 1 al 6
            for (int id = 1; id <= 6; id++) {
                File archivoEncontrado = buscarArchivoPorId(carpeta, id);

                if (archivoEncontrado != null) {
                    System.out.println("📂 Encontré para ID " + id + ": " + archivoEncontrado.getName());

                    // Subir a la Base de Datos
                    String sql = "UPDATE productos SET imagen = ? WHERE id_producto = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);

                    FileInputStream fis = new FileInputStream(archivoEncontrado);
                    ps.setBinaryStream(1, fis, (int) archivoEncontrado.length());
                    ps.setInt(2, id);

                    int filas = ps.executeUpdate();
                    if (filas > 0) {
                        System.out.println("   ✅ ¡FOTO CARGADA EXITOSAMENTE!");
                    }
                    fis.close();
                    ps.close();
                } else {
                    System.out.println("⚠️ ALERTA: No hay ningún archivo que empiece con '" + id + ".' en la carpeta.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("🏁 FIN DEL PROCESO");
    }

    // Método auxiliar que busca "1.jpg", "1.avif", "1.jpeg", etc.
    private static File buscarArchivoPorId(File carpeta, int id) {
        File[] archivos = carpeta.listFiles();
        if (archivos != null) {
            for (File f : archivos) {
                // Si el archivo empieza con el número y un punto (ej: "1.")
                if (f.isFile() && f.getName().startsWith(id + ".")) {
                    return f;
                }
            }
        }
        return null;
    }
}