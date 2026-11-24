package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String sender;
    private final boolean enabled;

    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String sender,
                        @Value("${worktrack.mail.enabled}") boolean enabled) {
        this.mailSender = mailSender;
        this.sender = sender;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void send(String recipient, String subject, String content) {
        if (!enabled) {
            throw ApiException.unavailable("O envio de e-mails está desativado neste ambiente.");
        }
        var message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (MailException error) {
            LOG.warn("Falha ao enviar e-mail ({})", error.getClass().getSimpleName());
            throw ApiException.unavailable("O serviço de e-mail está temporariamente indisponível.");
        }
    }
}
