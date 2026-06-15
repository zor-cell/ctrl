package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class AdminService {
  private final UserRepository userRepository;
  private final GmailEmailService gmailEmailService;
  private final TemplateEngine templateEngine;

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  public void deleteRequest(String username) {
    suspendAccount(username);
  }

  private void suspendAccount(String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));

    user.setSuspended(true);
    userRepository.save(user);
    sendEmail(user.getEmail());
  }

  private void sendEmail(String email) {

    if (!requireEmailVerification) {
      return;
    }

    try {

      Context context = new Context();

      context.setVariable("type", ContentTypeEnum.ACCOUNT.name());

      String htmlContent = templateEngine.process("actionNotificationTemplate", context);
      gmailEmailService.sendHtmlEmail(email, "An action has been taken against you", htmlContent);
    } catch (MailException e) {
      throw new CommunicationException("Failed to send email.");
    } catch (MessagingException me) {
      throw new CommunicationException("Failed to create email.");
    } catch (IOException e) {
      throw new CommunicationException("Failed to send or create email.");
    }
  }
}
