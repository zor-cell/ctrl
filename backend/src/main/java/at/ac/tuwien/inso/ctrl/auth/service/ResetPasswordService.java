package at.ac.tuwien.inso.ctrl.auth.service;

import at.ac.tuwien.inso.ctrl.auth.dto.PasswordResetEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.PasswordResetRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.PasswordReset;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidTokenException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ResetPasswordService {
  private final PasswordEncoder passwordEncoder;
  private final GmailService gmailService;
  private final TemplateEngine templateEngine;
  private final UserRepository userRepository;
  private final PasswordResetRepository passwordResetRepository;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Sends a password reset email to the user with the specified email address verifies if the email
   * exists deletes other reset tokens generates a token Generates email template Sends the email
   *
   * @param passwordResetEmailRequest the request containing the user's email address
   * @throws ResourceNotFoundException if no user exists with the provided email address
   * @throws CommunicationException if the email fails to send or if there is an error creating the
   *     email message
   * @see PasswordResetEmailRequest
   */
  @Transactional
  public void sendPasswordResetEmail(PasswordResetEmailRequest passwordResetEmailRequest) {
    LOGGER.info("Sending password reset email");
    try {
      Optional<User> user = userRepository.findByEmail(passwordResetEmailRequest.email());
      if (user.isEmpty()) {
        LOGGER.warn("User with email {} not found", passwordResetEmailRequest.email());
        throw new ResourceNotFoundException(
            "User with email " + passwordResetEmailRequest.email() + " not found");
      }

      passwordResetRepository.deleteUnusedTokensByUserId(user.get().getId());

      String token = UUID.randomUUID().toString();

      Context context = createContext(token);
      String htmlContent = templateEngine.process("passwordResetTemplate", context);

      gmailService.sendHtmlEmail(passwordResetEmailRequest.email(), "Reset CTRL Password!", htmlContent);

      LocalDateTime expiration = LocalDateTime.now().plusMinutes(30);
      PasswordReset passwordReset = new PasswordReset();
      passwordReset.setToken(token);
      passwordReset.setUser(user.get());
      passwordReset.setExpiresAt(expiration);
      passwordResetRepository.save(passwordReset);

    } catch (MailException e) {
      throw new CommunicationException(
          "Failed to send email. You can register again in 2 minutes.");
    } catch (MessagingException me) {
      throw new CommunicationException(
          "Failed to create email. You can register again in 2 minutes.");
    } catch (Exception e) {
      LOGGER.error("Failed to send or create password reset email", e);
      throw new CommunicationException("Failed to send or create email.");
    }
  }

  /**
   * Resets a user's password using a valid reset token.
   *
   * <p>This method validates the provided token and updates the user's password if valid. The token
   * is then marked as used to prevent reuse.
   *
   * @param passwordResetRequest the request containing the reset token and new password
   * @throws InvalidTokenException if the token is not found, has already been used, or has expired
   * @see PasswordResetRequest
   */
  @Transactional
  public void resetPassword(PasswordResetRequest passwordResetRequest) {
    PasswordReset passwordReset =
        passwordResetRepository
            .findByToken(passwordResetRequest.token())
            .orElseThrow(() -> new InvalidTokenException("Token not found"));

    if (passwordReset.isUsed()) {
      throw new InvalidTokenException("Token has already been used");
    }

    if (passwordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new InvalidTokenException("Token has expired");
    }

    User user = passwordReset.getUser();
    user.setPassword(passwordEncoder.encode(passwordResetRequest.password()));
    userRepository.save(user);

    passwordReset.setUsed(true);
    passwordResetRepository.save(passwordReset);
  }

  /**
   * Creates a Thymeleaf context for rendering the password reset email template.
   *
   * <p>Constructs the password reset redirect URL that will be included in the email. The URL
   * points to the frontend password reset page with the reset token as a query parameter.
   *
   * @param code the password reset token
   * @return a Thymeleaf {@link Context} containing the redirect URL variable
   */
  private Context createContext(String code) {
    Context context = new Context();
    context.setVariable("redirectUrl", "http://localhost:80/reset-password?token=" + code);
    return context;
  }
}
