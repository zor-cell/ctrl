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
import java.io.IOException;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final GmailEmailService gmailEmailService;
  private final TemplateEngine templateEngine;
  private final EmailVerificationRepository emailVerificationRepository;
  private final UserRepository userRepository;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  @Value("${spring.application.url:https://www.ctrl-ase.dev}")
  private String baseUrl;

  /**
   * Sends a verification email to the user with a verification code.
   *
   * @param sendEmailRequest the request containing username and email address
   * @throws UsernameNotFoundException if the username or email is not found
   * @throws UserAlreadyVerifiedException if the user is already verified
   * @throws CommunicationException if the email fails to send
   */
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
        LOGGER.info("username not found: {}", sendEmailRequest.email());

        return;
      }

      Optional<User> userByEmail = userRepository.findByEmail(sendEmailRequest.email());
      if (userByEmail.isEmpty()) {
        LOGGER.info("Email not found: {}", sendEmailRequest.email());

        return;
      }

      if (user.get() == null || userByEmail.get() == null) {
        return;
      }

      if (!user.get().getId().equals(userByEmail.get().getId())) {
        LOGGER.info("email wrong: {} {}", user.get().getId(), userByEmail.get().getId());

        return;
      }

      if (user.get().isEmailVerified()) {
        throw new UserAlreadyVerifiedException("User is already verified");
      }

      String code = generateCode(user.get().getPassword(), System.currentTimeMillis());
      Context context = createContext(code, sendEmailRequest);

      String htmlContent = templateEngine.process("verificationTemplate", context);

      gmailEmailService.sendHtmlEmail(sendEmailRequest.email(), "Welcome to CTRL", htmlContent);
      saveVerificationCode(sendEmailRequest, code);
    } catch (MailException e) {
      LOGGER.error("Failed to send email via Spring Mail", e);
      throw new CommunicationException("Failed to send email.");
    } catch (MessagingException me) {
      LOGGER.error("Failed to create email message", me);
      throw new CommunicationException("Failed to create email.");
    } catch (UsernameNotFoundException ignored) {

    } catch (IOException e) {
      LOGGER.error("Gmail API send failed", e);
      throw new CommunicationException("Failed to send or create email.");
    }
  }

  /**
   * Verifies the user's email by validating the provided verification code.
   *
   * @param emailVerificationRequest the request containing username and verification code
   * @return a response indicating verification success
   * @throws ResourceNotFoundException if the user or verification is not found
   * @throws InvalidEmailVerificationCodeException if the code is invalid or expired
   */
  public EmailVerificationResponse verifyEmail(EmailVerificationRequest emailVerificationRequest) {
    if (!requireEmailVerification) {
      return new EmailVerificationResponse(true);
    }

    User user =
        userRepository
            .findByUsername(emailVerificationRequest.username())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Verification failed. User does not exist or Token is invalid"));

    EmailVerification verification =
        emailVerificationRepository
            .findEmailVerificationByUser_Id(user.getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Verification failed. User does not exist or Token is invalid"));

    if (verification.getCode().equals(emailVerificationRequest.code())
        && verification.getExpiryTime().isAfter(LocalDateTime.now())) {
      emailVerificationRepository.delete(verification);
      user.setEmailVerified(true);
      userRepository.save(user);
      return new EmailVerificationResponse(true);
    } else {
      throw new InvalidEmailVerificationCodeException(
          "Verification failed. User does not exist or Token is invalid");
    }
  }

  /**
   * Creates a Thymeleaf context for the verification email template.
   *
   * @param code the verification code
   * @param data the email request containing username and email
   * @return a context containing verification code and redirect URL
   */
  private Context createContext(String code, SendEmailRequest data) {
    Context context = new Context();

    context.setVariable("verificationCode", code);
    context.setVariable(
        "redirectUrl", baseUrl + "/email_verification/" + data.username() + "/" + data.email());
    return context;
  }

  /**
   * Saves the verification code for the user, replacing any existing code.
   *
   * @param sendEmailRequest the request containing username and email
   * @param code the verification code to save
   */
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

  /**
   * Generates a 6-digit verification code using SHA-256 hash of password and timestamp. (created
   * with AI help)
   *
   * @param password the user's password
   * @param timeInMs the current time in milliseconds
   * @return a 6-digit verification code as a string
   */
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
