package com.mathew.gimnasio.servicios;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * SERVICIO DE CORREO ELECTRÓNICO (EMAIL SERVICE)
 * Esta clase se encarga de enviar correos automáticos desde nuestra aplicación.
 * Utiliza la librería Jakarta Mail y se conecta al servidor SMTP de Gmail
 * para hacer llegar los códigos de verificación a los nuevos clientes.
 */
public class EmailService {

    // Credenciales de la cuenta remitente (el correo oficial del gimnasio)
    // Se utiliza una "Contraseña de Aplicación" de Google para saltar la verificación en dos pasos
    private final String miCorreo = "mathewlara2006@gmail.com";
    private final String miPassword = "ozmr racb urap vtdv"; //

    /**
     * ENVIAR CÓDIGO DE VERIFICACIÓN
     * Configura la conexión con Google, arma el "sobre" virtual del correo,
     * le inserta el mensaje y lo despacha al destinatario.
     * @param destinatario El correo electrónico del usuario que se está registrando.
     * @param codigo El código numérico generado aleatoriamente en el controlador.
     */
    public void enviarCodigo(String destinatario, String codigo) {

        // 1. Configuración de las propiedades del servidor SMTP de Google
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true"); // Le decimos que vamos a iniciar sesión (requiere autenticación)
        props.put("mail.smtp.starttls.enable", "true"); // Activamos TLS para que la conexión viaje encriptada y segura
        props.put("mail.smtp.host", "smtp.gmail.com"); // La dirección del servidor de salida de correos de Google
        props.put("mail.smtp.port", "587"); // El puerto estándar que usa Gmail para conexiones TLS

        // 2. Sesión de seguridad y autenticación
        // Creamos una sesión en el servidor de correo usando nuestras credenciales
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(miCorreo, miPassword);
            }
        });

        try {
            // 3. Armado del mensaje (Como escribir una carta física)
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(miCorreo)); // Remitente: Quién envía la carta
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario)); // Destinatario: A quién va dirigida
            message.setSubject("Código de Verificación - Gimnasio"); // Asunto del correo

            // Cuerpo del correo con el código concatenado directamente en el texto
            message.setText("Hola,\n\nTu código de acceso es: " + codigo + "\n\nEste código expira en 5 minutos.");

            // 4. Envío final del correo
            // Transport.send toma el mensaje armado y lo dispara por la red
            Transport.send(message);
            System.out.println("Correo enviado correctamente a: " + destinatario);

        } catch (MessagingException e) {
            // Si el correo no sale (ej. no hay internet, credenciales bloqueadas, o puerto cerrado),
            // capturamos el error para que el servidor no se caiga y mostramos el problema.
            e.printStackTrace();
            System.out.println("Error enviando correo: " + e.getMessage());
        }
    }
}