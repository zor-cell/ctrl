package at.ac.tuwien.inso.ctrl.auth.service;

import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidEmailVerificationCodeException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.exception.UserAlreadyVerifiedException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
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

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final EmailVerificationRepository emailVerificationRepository;
  private final UserRepository userRepository;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  @Async
  public void sendVerificationEmail(SendEmailRequest sendEmailRequest) {
    LOGGER.info("Sending verification email to: {}", sendEmailRequest.email());
    if (!requireEmailVerification) {
      LOGGER.info("Email verification not required");
      return;
    }
    try {

      Optional<User> user = userRepository.findByUsername(sendEmailRequest.username());
      if (user.isEmpty()) {
        throw new UsernameNotFoundException("Username not found");
      }

      Optional<User> userByEmail = userRepository.findByEmail(sendEmailRequest.email());
      if (userByEmail.isEmpty()) {
        throw new UsernameNotFoundException("Email not found");
      }

      if (!user.get().equals(userByEmail.get())) {
        throw new ResourceNotFoundException("User not found for verification");
      }

      if (user.get().isEmailVerified()) {
        throw new UserAlreadyVerifiedException("User is already verified");
      }

      String code = generateCode(user.get().getPassword(), System.currentTimeMillis());
      Context context = createContext(code, sendEmailRequest);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      String htmlContent = templateEngine.process("verificationTemplate", context);

      helper.setTo(sendEmailRequest.email());
      helper.setSubject("Welcome to CTRL");
      helper.setText(htmlContent, true);

      mailSender.send(message);
      saveVerificationCode(sendEmailRequest, code);
    } catch (MailException e) {
      throw new CommunicationException(
          "Failed to send email.");
    } catch (MessagingException me) {
      throw new CommunicationException(
          "Failed to create email.");
    }
  }

  public EmailVerificationResponse verifyEmail(EmailVerificationRequest emailVerificationRequest) {
    if (!requireEmailVerification) {
      return new EmailVerificationResponse(true);
    }
    User user =
        userRepository
            .findByUsername(emailVerificationRequest.username())
            .orElseThrow(() -> new ResourceNotFoundException("User not found for verification"));
    EmailVerification verification =
        emailVerificationRepository
            .findEmailVerificationByUser_Id(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Verification not found for user"));

    if (verification.getCode().equals(emailVerificationRequest.code())
        && verification.getExpiryTime().isAfter(LocalDateTime.now())) {
      emailVerificationRepository.delete(verification);
      user.setEmailVerified(true);
      userRepository.save(user);
      return new EmailVerificationResponse(true);
    } else {
      throw new InvalidEmailVerificationCodeException("Invalid or expired verification code");
    }
  }

  private Context createContext(String code, SendEmailRequest data) {
    Context context = new Context();

    context.setVariable("verificationCode", code);
    context.setVariable(
        "redirectUrl",
        "http://localhost:80/email_verification/" + data.username() + "/" + data.email());
    return context;
  }

  private void saveVerificationCode(SendEmailRequest sendEmailRequest, String code) {
    User user =
        userRepository
            .findByUsername(sendEmailRequest.username())
            .orElseThrow(() -> new UsernameNotFoundException("User not found for verification"));

    emailVerificationRepository
        .findEmailVerificationByUser_Id(user.getId())
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

      int value =
          ((hash[0] & 0xFF) << 24)
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
