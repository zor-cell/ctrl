package at.ac.tuwien.inso.ctrl.auth.service;

import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final JavaMailSender mailSender;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Async
  public void sendVerificationEmail(RegisterRequest registerRequest) {
    LOGGER.info("Sending verification email to: {}", registerRequest.email());
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setTo(registerRequest.email());
      helper.setSubject("Welcome to CTRL");
      helper.setText("text", true);

      mailSender.send(message);
    } catch (MailException e) {
      throw new RuntimeException("Failed to send email", e);
    } catch (MessagingException me) {
      throw new RuntimeException("Failed to create Message", me);
    }
  }
}
