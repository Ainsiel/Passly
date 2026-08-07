package com.passly.notification.adapter.out.email;

import com.passly.notification.application.EmailSender;
import com.passly.notification.config.NotificationProperties;
import jakarta.mail.MessagingException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: envía el email vía {@code JavaMailSender} (SMTP). En
 * desarrollo apunta a Mailhog; el remitente se configura con
 * {@code passly.notification.email.from}.
 */
@Component
public class MailhogEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final NotificationProperties properties;

	public MailhogEmailSender(JavaMailSender mailSender, NotificationProperties properties) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	@Override
	public void send(String to, String subject, String htmlBody) {
		try {
			var mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
			helper.setFrom(properties.getEmail().getFrom());
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);
			mailSender.send(mimeMessage);
		} catch (MessagingException e) {
			throw new RuntimeException("No se pudo enviar el email del ticket a " + to, e);
		}
	}
}
