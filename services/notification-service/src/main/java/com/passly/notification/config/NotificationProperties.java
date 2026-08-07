package com.passly.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del contexto Notificaciones ({@code passly.notification.email.*}):
 * remitente, plantilla de asunto y URL de la vista "Mis reservas" de la web.
 */
@ConfigurationProperties(prefix = "passly.notification")
public class NotificationProperties {

	private final Email email = new Email();

	public Email getEmail() {
		return email;
	}

	public static class Email {

		private String from = "no-reply@passly.dev";

		/** {eventName} se sustituye por el nombre del evento. */
		private String subjectTemplate = "Tu ticket para {eventName}";

		private String myReservationsUrl = "https://passly.local/reservas";

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getSubjectTemplate() {
			return subjectTemplate;
		}

		public void setSubjectTemplate(String subjectTemplate) {
			this.subjectTemplate = subjectTemplate;
		}

		public String getMyReservationsUrl() {
			return myReservationsUrl;
		}

		public void setMyReservationsUrl(String myReservationsUrl) {
			this.myReservationsUrl = myReservationsUrl;
		}
	}
}
