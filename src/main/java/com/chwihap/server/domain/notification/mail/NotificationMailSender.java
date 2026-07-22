package com.chwihap.server.domain.notification.mail;

public interface NotificationMailSender {

    void send(String recipient, NotificationMailMessage message);

}
