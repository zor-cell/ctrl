package at.ac.tuwien.inso.ctrl.auth.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GmailEmailService {

  private final Gmail gmail;
  @Value("${spring.mail.username:test@gmail.com}")
  private String senderEmail;

  public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException, IOException {
    MimeMessage mimeMessage = createHtmlMimeMessage(to, subject, htmlBody);
    Message message = encodeMessage(mimeMessage);
    gmail.users().messages().send("me", message).execute();
  }

  private MimeMessage createHtmlMimeMessage(String to, String subject, String htmlBody)
      throws MessagingException {
    Session session = Session.getDefaultInstance(new Properties(), null);
    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(senderEmail));
    email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
    email.setSubject(subject);
    email.setContent(htmlBody, "text/html; charset=utf-8");
    return email;
  }

  private Message encodeMessage(MimeMessage mimeMessage) throws IOException, MessagingException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    mimeMessage.writeTo(buffer);
    byte[] rawBytes = buffer.toByteArray();
    String encodedEmail = Base64.getUrlEncoder().encodeToString(rawBytes);
    Message message = new Message();
    message.setRaw(encodedEmail);
    return message;
  }
}
