package at.ac.tuwien.inso.ctrl.auth.service;

import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidEmailVerificationCodeException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final EmailVerificationRepository emailVerificationRepository;
  private final UserRepository userRepository;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  @Async
  public void sendVerificationEmail(RegisterRequest registerRequest) {
    LOGGER.info("Sending verification email to: {}", registerRequest.email());
    if (!requireEmailVerification) {
      LOGGER.info("Email verification not required");
      return;
    }
    try {
      String code = generateCode(registerRequest.password(), System.currentTimeMillis());
      Context context = createContext(code);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      String htmlContent = templateEngine.process("verificationTemplate", context);

      helper.setTo(registerRequest.email());
      helper.setSubject("Welcome to CTRL");
      helper.setText(htmlContent, true);

      mailSender.send(message);
      saveVerificationCode(registerRequest, code);
    } catch (MailException e) {
      throw new CommunicationException("Failed to send email. You can register again in 2 minutes.");
    } catch (MessagingException me) {
      throw new CommunicationException("Failed to create email. You can register again in 2 minutes.");
    }
  }

  public EmailVerificationResponse verifyEmail(EmailVerificationRequest emailVerificationRequest) {
    if (!requireEmailVerification) {
      return new EmailVerificationResponse(true);
    }
    User user = userRepository.findByUsername(emailVerificationRequest.username())
        .orElseThrow(() -> new ResourceNotFoundException("User not found for verification"));
    EmailVerification verification = emailVerificationRepository.findEmailVerificationByUser_Id(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Verification not found for user"));

    if (verification.getCode().equals(emailVerificationRequest.code()) && verification.getExpiryTime().isAfter(LocalDateTime.now())) {
      emailVerificationRepository.delete(verification);
      user.setEmailVerified(true);
      userRepository.save(user);
      return new EmailVerificationResponse(true);
    } else {
      throw new InvalidEmailVerificationCodeException("Invalid or expired verification code");
    }
  }

  private Context createContext(String code) {
    Context context = new Context();

    context.setVariable("verificationCode", code);
    return context;
  }
  
  private void saveVerificationCode(RegisterRequest registerRequest, String code) {
    User user = userRepository.findByUsername(registerRequest.username())
            .orElseThrow(() -> new UsernameNotFoundException("User not found for verification"));

    emailVerificationRepository.findEmailVerificationByUser_Id(user.getId())
            .ifPresent(emailVerificationRepository::delete);

    EmailVerification verification = new EmailVerification();
    verification.setUser(user);
    verification.setCode(code);
    verification.setExpiryTime(LocalDateTime.now().plusMinutes(1));
    emailVerificationRepository.save(verification);
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
