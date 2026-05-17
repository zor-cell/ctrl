package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.user.dto.BusinessVerificationDecisionRequest;
import at.ac.tuwien.inso.ctrl.user.dto.BusinessVerificationResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.dto.VerificationStatusResponse;
import at.ac.tuwien.inso.ctrl.user.dto.VerificationType;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerification;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerificationStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.BusinessVerificationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class BusinessVerificationService {
  private final BusinessVerificationRepository businessVerificationRepository;
  private final UserMapper userMapper;
  private final UserRepository userRepository;
  private final GmailEmailService gmailEmailService;
  private final TemplateEngine templateEngine;

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  public void submitBusinessVerification(User currentUser) {
    if (businessVerificationRepository.existsByBusinessUser(currentUser)) {
      throw new ResourceAlreadyExistsException(
          "BusinessVerification", "userId", currentUser.getUsername());
    }

    BusinessVerification verification = new BusinessVerification();
    verification.setBusinessUser(currentUser);
    verification.setStatus(BusinessVerificationStatus.PENDING);
    businessVerificationRepository.save(verification);
  }

  public List<BusinessVerificationResponse> getAllPendingVerifications() {
    List<BusinessVerification> pendingVerifications =
        businessVerificationRepository.findAllByStatus(BusinessVerificationStatus.PENDING);
    return pendingVerifications.stream()
        .map(
            verification -> {
              User user = verification.getBusinessUser();
              UserDetailsResponse userDetail = userMapper.toUserDetailsResponse(user);
              return new BusinessVerificationResponse(userDetail, verification.getId());
            })
        .collect(Collectors.toList());
  }

  public void updateVerification(UUID requestId, BusinessVerificationDecisionRequest request) {
    BusinessVerification verification =
        businessVerificationRepository
            .findById(requestId)
            .orElseThrow(
                () ->
                    new ResourceAlreadyExistsException(
                        "BusinessVerification", "id", requestId.toString()));

    verification.setStatus(request.status());

    businessVerificationRepository.save(verification);

    if (requireEmailVerification) {
      sendUpdateEmail(verification);
    }

    if (request.status().equals(BusinessVerificationStatus.APPROVED)) {
      User user = verification.getBusinessUser();
      user.setVerified(true);
      userRepository.save(user);
    }
  }

  private void sendUpdateEmail(BusinessVerification verification) {
    User user = verification.getBusinessUser();
    if (user == null) {
      throw new ResourceAlreadyExistsException(
          "BusinessVerification", "userId", verification.getId().toString());
    }
    try {
      Context context = createContext(verification.getStatus());

      String htmlContent = templateEngine.process("businessAccountReview", context);

      gmailEmailService.sendHtmlEmail(user.getEmail(), "Business Verification Update", htmlContent);

    } catch (MailException e) {
      throw new CommunicationException("Failed to send email.");
    } catch (MessagingException me) {
      throw new CommunicationException("Failed to create email.");
    } catch (IOException e) {
      throw new CommunicationException("Failed to send or create email.");
    }
  }

  private Context createContext(BusinessVerificationStatus status) {
    Context context = new Context();

    context.setVariable("status", status.toString());
    return context;
  }

  public VerificationStatusResponse getVerificationStatus(User currentUser) {

    BusinessVerification ver = businessVerificationRepository.findByBusinessUser(currentUser);
    if (ver == null) {

      if (!currentUser.isVerified()) {
        return new VerificationStatusResponse(
            VerificationType.PERSONAL, BusinessVerificationStatus.NOT_VERIFIED);
      } else {
        return new VerificationStatusResponse(
            VerificationType.PERSONAL, BusinessVerificationStatus.APPROVED);
      }

    } else {
      return new VerificationStatusResponse(VerificationType.BUSINESS, ver.getStatus());
    }
  }
}
