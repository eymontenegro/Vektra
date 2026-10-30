package com.vektra.service;

import com.vektra.model.Ticket;
import com.vektra.util.TicketFormatter;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envía el ticket por correo (Gmail SMTP). Las credenciales nunca van en el
 * código: salen de las variables de entorno {@code MAIL_SENDER} y
 * {@code MAIL_PASSWORD}, o de {@code mail.sender} y {@code mail.password} en
 * {@code config.properties}.
 * <p>
 * El envío es lento: se debe llamar fuera del hilo de la interfaz.
 */
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final String CONFIG_FILE = "config.properties";

    private final String senderEmail;
    private final String senderPassword;

    /**
     * @param senderEmail    correo remitente
     * @param senderPassword contraseña de aplicación del remitente
     */
    public EmailService(String senderEmail, String senderPassword) {
        this.senderEmail = senderEmail;
        this.senderPassword = senderPassword;
    }

    /** Crea el servicio leyendo las credenciales de la configuración. */
    public static EmailService fromConfig() {
        Properties config = loadConfig();
        return new EmailService(
                readSetting(config, "MAIL_SENDER", "mail.sender"),
                readSetting(config, "MAIL_PASSWORD", "mail.password"));
    }

    /**
     * Envía el ticket al correo del pasajero.
     *
     * @return true si el correo salió; false si falta configuración o falló el envío
     */
    public boolean send(Ticket ticket) {
        if (senderEmail.isBlank() || senderPassword.isBlank()) {
            LOGGER.warning("No hay credenciales de correo configuradas; no se envió el ticket.");
            return false;
        }
        try {
            MimeMessage message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ticket.getPassengerEmail()));
            message.setSubject("Tu ticket de Vektra", "UTF-8");
            message.setContent(buildContent(ticket), "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "No se pudo enviar el ticket por correo", e);
            return false;
        }
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });
    }

    /** Arma el HTML del correo con tablas y estilos en línea (los clientes de correo ignoran flexbox). */
    private String buildContent(Ticket ticket) {
        NumberFormat money = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
        money.setMaximumFractionDigits(0);

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:20px;background:#1a1a2e;font-family:Arial,sans-serif;">
                <table align="center" width="420" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;">
                  <tr><td style="background:#f39c12;color:#ffffff;padding:24px;text-align:center;border-radius:16px 16px 0 0;">
                    <h2 style="margin:0;">Vektra</h2>
                    <p style="margin:4px 0 0;">Sistema de Metro Inteligente</p>
                  </td></tr>
                  <tr><td style="padding:24px;">
                    <table width="372" cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="text-align:left;"><div style="font-size:10px;color:#888;">Origen</div>
                          <div style="font-weight:bold;margin-top:4px;">%s</div></td>
                        <td style="text-align:right;"><div style="font-size:10px;color:#888;">Destino</div>
                          <div style="font-weight:bold;margin-top:4px;">%s</div></td>
                      </tr>
                    </table>
                    <hr style="border:0;border-top:2px dashed #eeeeee;margin:16px 0;">
                    <div style="margin-bottom:10px;"><b>Pasajero:</b> %s</div>
                    <div style="margin-bottom:10px;"><b>Fecha:</b> %s</div>
                    <div style="margin-bottom:10px;"><b>Código:</b> %s</div>
                    <div style="font-size:28px;font-weight:bold;color:#f39c12;text-align:center;">%s</div>
                    <hr style="border:0;border-top:2px dashed #eeeeee;margin:16px 0;">
                    <div style="text-align:center;font-size:12px;color:#777777;">Viktor dice: ¡Buen viaje!</div>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(ticket.getOriginName()),
                escapeHtml(ticket.getDestinationName()),
                escapeHtml(ticket.getPassengerName()),
                escapeHtml(TicketFormatter.formatDate(ticket.getPurchaseDate())),
                escapeHtml(ticket.getCode()),
                escapeHtml(money.format(ticket.getPrice())));
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static Properties loadConfig() {
        Properties config = new Properties();
        Path file = Path.of(CONFIG_FILE);
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                config.load(in);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "No se pudo leer " + CONFIG_FILE, e);
            }
        }
        return config;
    }

    private static String readSetting(Properties config, String envName, String key) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return config.getProperty(key, "").trim();
    }
}