package com.mathew.gimnasio.util;

import com.mathew.gimnasio.configuracion.ConfiguracionEnv;
import com.mathew.gimnasio.configuracion.ConexionDB;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

/**
 * Herramienta de carga a Cloudinary: sube imágenes locales a Cloudinary y guarda la URL en la BD.
 * Ruta configurable por variable de entorno IMAGENES_GIMNASIO_PATH.
 * Requiere CLOUDINARY_URL.
 */
public class CargadorFotosDB {

    public static void main(String[] args) {
        String rutaCarpeta = ConfiguracionEnv.get("IMAGENES_GIMNASIO_PATH", "");
        if (rutaCarpeta.isEmpty()) {
            System.err.println("Definir IMAGENES_GIMNASIO_PATH (ej. C:/Imagenes Gimnasio/)");
            return;
        }
        
        String cloudinaryUrl = ConfiguracionEnv.get("CLOUDINARY_URL", "");
        if (cloudinaryUrl.isEmpty()) {
            System.err.println("Definir CLOUDINARY_URL para poder subir las imagenes a Cloudinary");
            return;
        }

        File carpeta = new File(rutaCarpeta);
        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);

        try (Connection conn = ConexionDB.getConnection()) {
            // Buscamos productos del 1 al 6
            for (int id = 1; id <= 6; id++) {
                File archivoEncontrado = buscarArchivoPorId(carpeta, id);

                if (archivoEncontrado != null) {
                    System.out.println("📂 Encontré para ID " + id + ": " + archivoEncontrado.getName());

                    try {
                        // Subir a Cloudinary
                        System.out.println("   ☁️ Subiendo a Cloudinary...");
                        Map uploadResult = cloudinary.uploader().upload(archivoEncontrado, ObjectUtils.emptyMap());
                        String secureUrl = (String) uploadResult.get("secure_url");
                        System.out.println("   ✅ URL de Cloudinary: " + secureUrl);

                        // Actualizar la Base de Datos con la URL
                        String sql = "UPDATE productos SET imagen_url = ? WHERE id_producto = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, secureUrl);
                            ps.setInt(2, id);

                            int filas = ps.executeUpdate();
                            if (filas > 0) {
                                System.out.println("   ✅ ¡FOTO ENLAZADA EXITOSAMENTE!");
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("   ❌ Error subiendo a Cloudinary o actualizando DB: " + ex.getMessage());
                        ex.printStackTrace();
                    }
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