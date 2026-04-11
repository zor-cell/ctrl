package at.ac.tuwien.inso.ctrl.auth.service;

import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Async
  public void sendVerificationEmail(RegisterRequest registerRequest) {
    LOGGER.info("Sending verification email to: {}", registerRequest.email());
    try {
      Context context = createContext(registerRequest);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      String htmlContent = templateEngine.process("verificationTemplate", context);

      helper.setTo(registerRequest.email());
      helper.setSubject("Welcome to CTRL");
      helper.setText(htmlContent, true);

      mailSender.send(message);
    } catch (MailException e) {
      throw new RuntimeException("Failed to send email", e);
    } catch (MessagingException me) {
      throw new RuntimeException("Failed to create Message", me);
    }
  }

  private Context createContext(RegisterRequest registerRequest) {
    Context context = new Context();

    context.setVariable("verificationCode", generateCode(registerRequest.password(), System.currentTimeMillis()));
    return context;
  }

  private String generateCode(String password, long timeInMs) {
    String input = password + ":" + timeInMs;

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      int value = ((hash[0] & 0xFF) << 24)
              | ((hash[1] & 0xFF) << 16)
              | ((hash[2] & 0xFF) << 8)
              | (hash[3] & 0xFF);

      int code = Math.floorMod(value, 1_000_000);
      return String.format("%06d", code);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
  
}
