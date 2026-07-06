package com.mathew.gimnasio.servicios;

import com.mathew.gimnasio.configuracion.ConfiguracionEnv;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Servicio de correo usando la API transaccional de Brevo (v3) vía HTTP.
 * Requiere la variable de entorno BREVO_API_KEY.
 */
public class EmailService {

    private final String apiKey;
    private final String remitente;

    public EmailService() {
        this.apiKey = ConfiguracionEnv.get("BREVO_API_KEY", "");
        // Brevo requiere que el remitente esté verificado en su plataforma.
        this.remitente = ConfiguracionEnv.get("BREVO_SENDER_EMAIL", "onboarding@resend.dev");
    }

    public void enviarCodigo(String destinatario, String codigo) {
        if (apiKey.isEmpty()) {
            System.err.println("BREVO_API_KEY no configurado; correo no enviado.");
            return;
        }

        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{"
                    + "\"sender\": {\"name\": \"Gimnasio\", \"email\": \"" + remitente + "\"},"
                    + "\"to\": [{\"email\": \"" + destinatario + "\"}],"
                    + "\"subject\": \"Código de Verificación - Gimnasio\","
                    + "\"htmlContent\": \"<p>Hola,</p><p>Tu código de acceso es: <strong>" + codigo
                    + "</strong></p><p>Este código expira en 5 minutos.</p>\""
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                System.out.println("Correo enviado correctamente a: " + destinatario + " (Brevo API)");
            } else {
                System.err.println("Error enviando correo (Brevo API HTTP " + code + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Excepción enviando correo por HTTP: " + e.getMessage());
        }
    }
}
