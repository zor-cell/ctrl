package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
import at.ac.tuwien.inso.ctrl.user.service.BusinessVerificationService;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
public class BusinessVerificationServiceUnitTest {
  @Mock private GmailEmailService gmailEmailService;
  @Mock private TemplateEngine templateEngine;
  @Mock private BusinessVerificationRepository businessVerificationRepository;
  @Mock private UserMapper userMapper;
  @Mock private UserRepository userRepository;

  @InjectMocks private BusinessVerificationService businessVerificationService;

  private User testUser;
  private User testUser1;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(UUID.randomUUID())
            .username("testUser")
            .password("hashedPassword")
            .email("test@test.com")
            .emailVerified(true)
            .isVerified(false)
            .build();

    testUser1 =
        User.builder()
            .id(UUID.randomUUID())
            .username("testUser2")
            .password("hashedPassword2")
            .email("test2@test.com")
            .emailVerified(true)
            .isVerified(false)
            .build();
  }

  @Test
  void whenBusinessVerificationAlreadyExists_thenThrowResourceAlreadyExistsException() {
    when(businessVerificationRepository.existsByBusinessUser(testUser)).thenReturn(true);

    assertThrows(
        ResourceAlreadyExistsException.class,
        () -> businessVerificationService.submitBusinessVerification(testUser));

    verify(businessVerificationRepository, never()).save(any(BusinessVerification.class));
  }

  @Test
  void whenBusinessVerificationDoesNotExist_thenSaveVerification() {
    when(businessVerificationRepository.existsByBusinessUser(testUser)).thenReturn(false);

    businessVerificationService.submitBusinessVerification(testUser);

    ArgumentCaptor<BusinessVerification> verificationCaptor =
        ArgumentCaptor.forClass(BusinessVerification.class);
    verify(businessVerificationRepository).save(verificationCaptor.capture());

    BusinessVerification savedVerification = verificationCaptor.getValue();
    assertEquals(testUser, savedVerification.getBusinessUser());
    assertEquals(BusinessVerificationStatus.PENDING, savedVerification.getStatus());
  }

  @Test
  void whenGetAllPendingVerifications_withNoPending_shouldReturnEmptyList() {
    when(businessVerificationRepository.findAllByStatus(BusinessVerificationStatus.PENDING))
        .thenReturn(List.of());

    List<BusinessVerificationResponse> verifications =
        businessVerificationService.getAllPendingVerifications();

    assertNotNull(verifications);
    assertEquals(0, verifications.size());
  }

  @Test
  void whenGetAllPendingVerifications_withOnePending_shouldContainVerification() {
    UserDetailsResponse userDetails =
        new UserDetailsResponse(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getEmail(),
            testUser.getName(),
            testUser.getBio(),
            testUser.getBirthdate(),
            testUser.getProfilePicture(),
            testUser.getCreatedAt(),
            testUser.isEmailVerified());

    BusinessVerification verification =
        BusinessVerification.builder()
            .status(BusinessVerificationStatus.PENDING)
            .id(UUID.randomUUID())
            .businessUser(testUser)
            .build();

    List<BusinessVerification> listVer = List.of(verification);

    BusinessVerificationResponse ver =
        new BusinessVerificationResponse(userDetails, verification.getId());

    when(userMapper.toUserDetailsResponse(testUser)).thenReturn(ver.user());
    when(businessVerificationRepository.findAllByStatus(BusinessVerificationStatus.PENDING))
        .thenReturn(listVer);

    List<BusinessVerificationResponse> verifications =
        businessVerificationService.getAllPendingVerifications();

    assertNotNull(verifications);
    assertEquals(1, verifications.size());
    assertEquals(ver, verifications.getFirst());
  }

  @Test
  void whenGetAllPendingVerifications_withTwoPending_shouldContainBothVerification() {
    UserDetailsResponse userDetails =
        new UserDetailsResponse(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getEmail(),
            testUser.getName(),
            testUser.getBio(),
            testUser.getBirthdate(),
            testUser.getProfilePicture(),
            testUser.getCreatedAt(),
            testUser.isEmailVerified());

    UserDetailsResponse userDetails1 =
        new UserDetailsResponse(
            testUser1.getId(),
            testUser1.getUsername(),
            testUser1.getEmail(),
            testUser1.getName(),
            testUser1.getBio(),
            testUser1.getBirthdate(),
            testUser1.getProfilePicture(),
            testUser1.getCreatedAt(),
            testUser1.isEmailVerified());

    BusinessVerification verification =
        BusinessVerification.builder()
            .status(BusinessVerificationStatus.PENDING)
            .id(UUID.randomUUID())
            .businessUser(testUser)
            .build();

    BusinessVerification verification1 =
        BusinessVerification.builder()
            .status(BusinessVerificationStatus.PENDING)
            .id(UUID.randomUUID())
            .businessUser(testUser1)
            .build();

    List<BusinessVerification> listVer = List.of(verification, verification1);

    BusinessVerificationResponse ver =
        new BusinessVerificationResponse(userDetails, verification.getId());
    BusinessVerificationResponse ver1 =
        new BusinessVerificationResponse(userDetails1, verification1.getId());

    when(userMapper.toUserDetailsResponse(testUser)).thenReturn(ver.user());
    when(userMapper.toUserDetailsResponse(testUser1)).thenReturn(ver1.user());
    when(businessVerificationRepository.findAllByStatus(BusinessVerificationStatus.PENDING))
        .thenReturn(listVer);

    List<BusinessVerificationResponse> verifications =
        businessVerificationService.getAllPendingVerifications();

    assertNotNull(verifications);
    assertEquals(2, verifications.size());
    assertTrue(verifications.contains(ver));
    assertTrue(verifications.contains(ver1));
  }

  @Test
  void whenUpdateVerificationRequestNotFound_thenThrowResourceAlreadyExistsException() {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", false);
    UUID requestId = UUID.randomUUID();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.empty());

    assertThrows(
        ResourceAlreadyExistsException.class,
        () ->
            businessVerificationService.updateVerification(
                requestId,
                new BusinessVerificationDecisionRequest(BusinessVerificationStatus.PENDING)));

    verify(businessVerificationRepository, never()).save(any(BusinessVerification.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void whenUpdateVerificationApproved_thenUserIsVerifiedAndSaved() {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", false);
    UUID requestId = UUID.randomUUID();
    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(testUser)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));

    businessVerificationService.updateVerification(
        requestId, new BusinessVerificationDecisionRequest(BusinessVerificationStatus.APPROVED));

    ArgumentCaptor<BusinessVerification> verificationCaptor =
        ArgumentCaptor.forClass(BusinessVerification.class);
    verify(businessVerificationRepository).save(verificationCaptor.capture());
    assertEquals(BusinessVerificationStatus.APPROVED, verificationCaptor.getValue().getStatus());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertTrue(userCaptor.getValue().isVerified());
  }

  @Test
  void whenUpdateVerificationRejected_thenUserNotVerifiedAndNotSaved() {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", false);
    UUID requestId = UUID.randomUUID();
    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(testUser)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));

    businessVerificationService.updateVerification(
        requestId, new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED));

    ArgumentCaptor<BusinessVerification> verificationCaptor =
        ArgumentCaptor.forClass(BusinessVerification.class);
    verify(businessVerificationRepository).save(verificationCaptor.capture());
    assertEquals(BusinessVerificationStatus.REJECTED, verificationCaptor.getValue().getStatus());

    verify(userRepository, never()).save(any(User.class));
    assertFalse(testUser.isVerified());
  }

  @Test
  void whenUpdateVerificationWithEmailEnabled_thenSendUpdateEmail() throws Exception {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", true);
    UUID requestId = UUID.randomUUID();
    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(testUser)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));
    when(templateEngine.process(eq("businessAccountReview"), any(Context.class)))
        .thenReturn("<html>update</html>");

    businessVerificationService.updateVerification(
        requestId, new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED));

    verify(gmailEmailService)
        .sendHtmlEmail(
            eq(testUser.getEmail()), eq("Business Verification Update"), any(String.class));
  }

  @Test
  void whenUpdateVerificationEmailSendThrowsMailException_thenThrowCommunicationException()
      throws MessagingException, IOException {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", true);
    UUID requestId = UUID.randomUUID();
    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(testUser)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));
    when(templateEngine.process(eq("businessAccountReview"), any(Context.class)))
        .thenReturn("<html>update</html>");
    doThrow(new MailException("mail failed") {})
        .when(gmailEmailService)
        .sendHtmlEmail(any(String.class), any(String.class), any(String.class));

    assertThrows(
        CommunicationException.class,
        () ->
            businessVerificationService.updateVerification(
                requestId,
                new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED)));
  }

  @Test
  void whenUpdateVerificationEmailSendThrowsMessagingException_thenThrowMessagingException()
      throws MessagingException, IOException {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", true);
    UUID requestId = UUID.randomUUID();
    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(testUser)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));
    when(templateEngine.process(eq("businessAccountReview"), any(Context.class)))
        .thenReturn("<html>update</html>");
    doThrow(new MessagingException("mail failed") {})
        .when(gmailEmailService)
        .sendHtmlEmail(any(String.class), any(String.class), any(String.class));

    assertThrows(
        CommunicationException.class,
        () ->
            businessVerificationService.updateVerification(
                requestId,
                new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED)));
  }

  @Test
  void whenUpdateVerificationEmailSendThrowsIOExceptionException_thenThrowCommunicationException()
      throws MessagingException, IOException {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", true);
    UUID requestId = UUID.randomUUID();
    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(testUser)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));
    when(templateEngine.process(eq("businessAccountReview"), any(Context.class)))
        .thenReturn("<html>update</html>");
    doThrow(new IOException("mail failed") {})
        .when(gmailEmailService)
        .sendHtmlEmail(any(String.class), any(String.class), any(String.class));

    assertThrows(
        CommunicationException.class,
        () ->
            businessVerificationService.updateVerification(
                requestId,
                new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED)));
  }

  @Test
  void whenUpdateVerificationWithNullUser_thenThrowResourceAlreadyExistsException() {
    ReflectionTestUtils.setField(businessVerificationService, "requireEmailVerification", true);
    UUID requestId = UUID.randomUUID();

    BusinessVerification verification =
        BusinessVerification.builder()
            .id(requestId)
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(null)
            .build();

    when(businessVerificationRepository.findById(requestId)).thenReturn(Optional.of(verification));

    assertThrows(
        ResourceAlreadyExistsException.class,
        () ->
            businessVerificationService.updateVerification(
                requestId,
                new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED)));
  }

  @Test
  void whenGetVerificationStatusWithVerificationApproved_thenReturnApproved() {
    BusinessVerification ver =
        BusinessVerification.builder()
            .businessUser(testUser)
            .status(BusinessVerificationStatus.APPROVED)
            .id(UUID.randomUUID())
            .build();

    when(businessVerificationRepository.findByBusinessUser(testUser)).thenReturn(ver);

    VerificationStatusResponse verResponse =
        businessVerificationService.getVerificationStatus(testUser);

    assertEquals(BusinessVerificationStatus.APPROVED, verResponse.status());
    assertEquals(VerificationType.BUSINESS, verResponse.type());
  }

  @Test
  void whenGetVerificationStatusWithVerificationRejected_thenReturnApproved() {
    BusinessVerification ver =
        BusinessVerification.builder()
            .businessUser(testUser)
            .status(BusinessVerificationStatus.REJECTED)
            .id(UUID.randomUUID())
            .build();

    when(businessVerificationRepository.findByBusinessUser(testUser)).thenReturn(ver);

    VerificationStatusResponse verResponse =
        businessVerificationService.getVerificationStatus(testUser);

    assertEquals(BusinessVerificationStatus.REJECTED, verResponse.status());
    assertEquals(VerificationType.BUSINESS, verResponse.type());
  }

  @Test
  void whenGetVerificationStatusWithVerifiedUser_thenReturnApproved() {

    User testUser2 =
        User.builder()
            .id(UUID.randomUUID())
            .username("testUser3")
            .password("hashedPassword3")
            .email("test3@test.com")
            .emailVerified(true)
            .isVerified(true)
            .build();

    when(businessVerificationRepository.findByBusinessUser(testUser2)).thenReturn(null);

    VerificationStatusResponse verResponse =
        businessVerificationService.getVerificationStatus(testUser2);

    assertEquals(BusinessVerificationStatus.APPROVED, verResponse.status());
    assertEquals(VerificationType.PERSONAL, verResponse.type());
  }

  @Test
  void whenGetVerificationStatusWithNotVerifiedUser_thenReturnApproved() {

    User testUser2 =
        User.builder()
            .id(UUID.randomUUID())
            .username("testUser3")
            .password("hashedPassword3")
            .email("test3@test.com")
            .emailVerified(true)
            .isVerified(false)
            .build();

    when(businessVerificationRepository.findByBusinessUser(testUser2)).thenReturn(null);

    VerificationStatusResponse verResponse =
        businessVerificationService.getVerificationStatus(testUser2);

    assertEquals(BusinessVerificationStatus.NOT_VERIFIED, verResponse.status());
    assertEquals(VerificationType.PERSONAL, verResponse.type());
  }
}
