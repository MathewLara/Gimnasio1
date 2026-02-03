package com.mathew.gimnasio.servicios;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private final String miCorreo = "mathewlara2006@gmail.com";
    private final String miPassword = "ozmr racb urap vtdv"; //

    public void enviarCodigo(String destinatario, String codigo) {
        // Configuración del servidor de Google
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Sesión de seguridad
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(miCorreo, miPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(miCorreo));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Código de Verificación - Gimnasio");
            message.setText("Hola,\n\nTu código de acceso es: " + codigo + "\n\nEste código expira en 5 minutos.");

            Transport.send(message);
            System.out.println("Correo enviado correctamente a: " + destinatario);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error enviando correo: " + e.getMessage());
        }
    }
}