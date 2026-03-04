package com.mathew.gimnasio.servicios;

import com.mathew.gimnasio.configuracion.ConfiguracionEnv;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Servicio de correo usando la API de Resend vía HTTP.
 * Requiere la variable de entorno RESEND_API_KEY.
 */
public class EmailService {

    private final String apiKey;
    private final String remitente;

    public EmailService() {
        this.apiKey = ConfiguracionEnv.get("RESEND_API_KEY", "");
        // Resend en su plan gratuito obliga a que el remitente sea exactamente este:
        this.remitente = "onboarding@resend.dev";
    }

    public void enviarCodigo(String destinatario, String codigo) {
        if (apiKey.isEmpty()) {
            System.err.println("RESEND_API_KEY no configurado; correo no enviado.");
            return;
        }

        try {
            URL url = new URL("https://api.resend.com/emails");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{"
                    + "\"from\": \"Gimnasio <" + remitente + ">\","
                    + "\"to\": [\"" + destinatario + "\"],"
                    + "\"subject\": \"Código de Verificación - Gimnasio\","
                    + "\"html\": \"<p>Hola,</p><p>Tu código de acceso es: <strong>" + codigo
                    + "</strong></p><p>Este código expira en 5 minutos.</p>\""
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("Correo enviado correctamente a: " + destinatario + " (Resend API)");
            } else {
                System.err.println("Error enviando correo (Resend API HTTP " + code + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Excepción enviando correo por HTTP: " + e.getMessage());
        }
    }
}
