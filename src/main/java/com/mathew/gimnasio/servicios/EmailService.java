package com.mathew.gimnasio.servicios;

import com.mathew.gimnasio.configuracion.ConfiguracionEnv;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Servicio de correo. Credenciales desde variables de entorno:
 * MAIL_USER, MAIL_PASSWORD (no subir al repositorio).
 */
public class EmailService {

    private final String miCorreo;
    private final String miPassword;

    public EmailService() {
        this.miCorreo = ConfiguracionEnv.get("MAIL_USER", "");
        this.miPassword = ConfiguracionEnv.get("MAIL_PASSWORD", "");
    }

    public void enviarCodigo(String destinatario, String codigo) {
        if (miCorreo.isEmpty() || miPassword.isEmpty()) {
            System.err.println("MAIL_USER y MAIL_PASSWORD no configurados; correo no enviado.");
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");

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
            System.err.println("Error enviando correo: " + e.getMessage());
        }
    }
}
