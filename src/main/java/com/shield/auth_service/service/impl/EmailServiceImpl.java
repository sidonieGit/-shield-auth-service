package com.shield.auth_service.service.impl;

import com.shield.auth_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendActivationEmail(String email, String token) {
        String link = "https://localhost:4200/auth/activate?token=" + token;
        String htmlContent = "<h1>Bienvenue sur Shield</h1>" +
                "<p>Cliquez sur le lien ci-dessous pour activer votre compte :</p>" +
                "<a href='" + link + "'>Activer mon compte</a>";

        sendHtmlEmail(email, "Activation de votre compte Shield", htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true = format HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    @Override
    public void sendResetPasswordEmail(String email, String token) {
        sendHtmlEmail(email, "Réinitialisation de mot de passe", "Votre token : " + token);
    }
}