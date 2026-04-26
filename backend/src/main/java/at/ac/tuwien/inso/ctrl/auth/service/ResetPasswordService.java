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
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResetPasswordService {
  private final PasswordEncoder passwordEncoder;
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final UserRepository userRepository;
  private final PasswordResetRepository passwordResetRepository;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Transactional
  public void sendPasswordResetEmail(PasswordResetEmailRequest passwordResetEmailRequest) {
    LOGGER.info("Sending password reset email");
    try {
      Optional<User> user = userRepository.findByEmail(passwordResetEmailRequest.email());
      if (user.isEmpty()) {
        LOGGER.warn("User with email {} not found", passwordResetEmailRequest.email());
        throw new ResourceNotFoundException("User with email " + passwordResetEmailRequest.email() + " not found");

      }

      passwordResetRepository.deleteUnusedTokensByUserId(user.get().getId());


      String token = UUID.randomUUID().toString();

      Context context = createContext(token);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      String htmlContent = templateEngine.process("passwordResetTemplate", context);

      helper.setTo(passwordResetEmailRequest.email());
      helper.setSubject("Reset CTRL Password!");
      helper.setText(htmlContent, true);

      mailSender.send(message);

      LocalDateTime expiration = LocalDateTime.now().plusMinutes(30);
      PasswordReset passwordReset = new PasswordReset();
      passwordReset.setToken(token);
      passwordReset.setUser(user.get());
      passwordReset.setExpiresAt(expiration);
      passwordResetRepository.save(passwordReset);

    } catch (MailException e) {
      throw new CommunicationException("Failed to send email. You can register again in 2 minutes.");
    } catch (MessagingException me) {
      throw new CommunicationException("Failed to create email. You can register again in 2 minutes.");
    }
  }

  @Transactional
  public void resetPassword(PasswordResetRequest passwordResetRequest) {
    PasswordReset passwordReset = passwordResetRepository.findByToken(passwordResetRequest.token())
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


  private Context createContext(String code) {
    Context context = new Context();
    context.setVariable("redirectUrl", "http://localhost:80/reset-password?token=" + code);
    return context;
  }
}
