package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.chat.ChatService;
import at.ac.tuwien.inso.ctrl.chat.repository.ReactionRepository;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.dto.UpdatePasswordRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateUsernameRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.dto.VerifyIdentityRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.BusinessVerificationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import at.ac.tuwien.inso.ctrl.user.service.IdAustriaOidcService;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {

  @Mock private UserRepository userRepository;

  @Mock private UserMapper userMapper;

  @Mock private RelationshipMapper socialMapper;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private ReportService reportService;

  @Mock private JwtService jwtService;

  @Mock private IdAustriaOidcService idAustriaOidcService;

  @Mock private FriendRequestRepository friendRequestRepository;

  @Mock private FriendRelationRepository friendRelationRepository;

  @Mock private FollowRelationRepository followRelationRepository;

  @Mock private BusinessVerificationRepository businessVerificationRepository;

  @Mock private FileStorageService fileStorageService;

  @Mock private FollowService followService;

  @Mock private BlockService blockService;

  @Mock private EmailVerificationRepository emailVerificationRepository;

  @Mock private PasswordResetRepository passwordResetRepository;

  @Mock private PostRepository postRepository;

  @Mock private PostVoteRepository postVoteRepository;

  @Mock private SourceRepository sourceRepository;

  @Mock private SourceVoteRepository sourceVoteRepository;

  @Mock private CommentRepository commentRepository;

  @Mock private ReactionRepository reactionRepository;

  @Mock private SeenPostRepository seenPostRepository;

  @Mock private AlgorithmRepository algorithmRepository;

  @Mock private ChatService chatService;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testUser");
    testUser.setEmail("test@domain.com");
    testUser.setName("Original Name");
    testUser.setPassword("encodedOldPassword");
    testUser.setVerified(false);
    testUser.setBirthdate(LocalDate.of(1990, 1, 1));
    testUser.setBio("bio");
  }

  @Test
  void getUser_ShouldReturnUserDetails_WhenUserExists() {
    UserDetailsResponse mockResponse =
        new UserDetailsResponse(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getName(),
            testUser.getBio(),
            testUser.getEmail(),
            testUser.getBirthdate(),
            null,
            Instant.now(),
            false);
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(userMapper.toUserDetailsResponse(testUser)).thenReturn(mockResponse);

    UserDetailsResponse result = userService.getUser(testUser.getId());

    assertEquals(testUser.getUsername(), result.username());
    verify(userRepository).findById(testUser.getId());
  }

  @Test
  void getUser_ShouldThrowException_WhenUserDoesNotExist() {
    UUID unknownId = UUID.randomUUID();
    when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class, () -> userService.getUser(unknownId));
  }

  @Test
  void getPublicProfile_withExistingUser_returnsPublicProfile() {
    var response =
        new at.ac.tuwien.inso.ctrl.user.dto.PublicUserProfileResponse(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getName(),
            testUser.getBio(),
            "http://localhost:3900/ctrl-test-media/users/profile-pictures/avatar.png",
            false,
            false,
            true,
            false);
    when(userRepository.findByUsernameAndDeletedFalse("testUser"))
        .thenReturn(Optional.of(testUser));
    when(reportService.wasReportedByUser(
            testUser, ContentTypeEnum.ACCOUNT, String.valueOf(testUser.getId())))
        .thenReturn(false);
    when(blockService.inBlockedRelationship(testUser, testUser)).thenReturn(false);
    when(userMapper.toPublicUserProfileResponse(testUser, true, false)).thenReturn(response);

    var result = userService.getPublicProfile("testUser", testUser);

    assertEquals(response, result);
    verify(userRepository).findByUsernameAndDeletedFalse("testUser");
    verify(blockService).inBlockedRelationship(testUser, testUser);
    verify(userMapper).toPublicUserProfileResponse(testUser, true, false);
  }

  @Test
  void getPublicProfile_withMissingUser_throwsUsernameNotFoundException() {
    when(userRepository.findByUsernameAndDeletedFalse("missing")).thenReturn(Optional.empty());

    assertThrows(
        UsernameNotFoundException.class, () -> userService.getPublicProfile("missing", testUser));
  }

  @Test
  void getPublicProfile_whenUserIsBlocked_throwsUsernameNotFoundException() {
    User otherUser = createUser("otherUser");
    when(userRepository.findByUsernameAndDeletedFalse("otherUser"))
        .thenReturn(Optional.of(otherUser));
    when(blockService.inBlockedRelationship(testUser, otherUser)).thenReturn(true);

    assertThrows(
        UsernameNotFoundException.class, () -> userService.getPublicProfile("otherUser", testUser));
    verify(userRepository).findByUsernameAndDeletedFalse("otherUser");
    verify(blockService).inBlockedRelationship(testUser, otherUser);
  }

  @Test
  void updateUser_ShouldUpdateSuccessfully_WhenValidRequest() {
    UpdateProfileRequest request =
        new UpdateProfileRequest("New Name", "new@domain.com", "New bio", LocalDate.of(1995, 5, 5));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("new@domain.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userService.updateUser(testUser, request);

    assertEquals("New Name", testUser.getName());
    assertEquals("new@domain.com", testUser.getEmail());
    assertEquals(LocalDate.of(1995, 5, 5), testUser.getBirthdate());
    verify(userRepository).save(testUser);
  }

  @Test
  void updateUser_ShouldThrowException_WhenNewEmailIsAlreadyTaken() {
    UpdateProfileRequest request =
        new UpdateProfileRequest(
            "New Name", "taken@domain.com", "New bio", LocalDate.of(1990, 1, 1));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(userRepository.existsByEmail("taken@domain.com")).thenReturn(true);

    assertThrows(
        ResourceAlreadyExistsException.class, () -> userService.updateUser(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void updateUser_ShouldThrowException_WhenVerifiedUserChangesName() {
    testUser.setVerified(true);
    UpdateProfileRequest request =
        new UpdateProfileRequest(
            "Different Name", "test@domain.com", "New bio", LocalDate.of(1990, 1, 1));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    assertThrows(InvalidRequestException.class, () -> userService.updateUser(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void updateUser_ShouldThrowException_WhenVerifiedUserChangesBirthdate() {
    testUser.setVerified(true);
    UpdateProfileRequest request =
        new UpdateProfileRequest(
            "Original Name", "test@domain.com", "New bio", LocalDate.of(1995, 5, 5));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    assertThrows(InvalidRequestException.class, () -> userService.updateUser(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void updatePassword_ShouldUpdate_WhenOldPasswordIsCorrect() {
    UpdatePasswordRequest request = new UpdatePasswordRequest("oldPass", "newPass");
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("oldPass", "encodedOldPassword")).thenReturn(true);
    when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPassword");

    userService.updatePassword(testUser, request);

    assertEquals("encodedNewPassword", testUser.getPassword());
    verify(userRepository).save(testUser);
  }

  @Test
  void updatePassword_ShouldThrowException_WhenOldPasswordIsIncorrect() {
    UpdatePasswordRequest request = new UpdatePasswordRequest("wrongPass", "newPass");
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("wrongPass", "encodedOldPassword")).thenReturn(false);

    assertThrows(
        InvalidPasswordException.class, () -> userService.updatePassword(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void updateUsername_ShouldUpdateAndReturnToken_WhenUsernameIsAvailable() {
    UpdateUsernameRequest request = new UpdateUsernameRequest("newUsername");
    when(userRepository.existsByUsername("newUsername")).thenReturn(false);
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(jwtService.generateToken(testUser)).thenReturn("mockJwtToken");

    AuthResponse response = userService.updateUsername(testUser, request);

    assertEquals("newUsername", testUser.getUsername());
    assertEquals("mockJwtToken", response.token());
    verify(userRepository).save(testUser);
  }

  @Test
  void updateUsername_ShouldThrowException_WhenUsernameIsTaken() {
    UpdateUsernameRequest request = new UpdateUsernameRequest("takenUsername");
    when(userRepository.existsByUsername("takenUsername")).thenReturn(true);

    assertThrows(
        ResourceAlreadyExistsException.class, () -> userService.updateUsername(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void updateProfilePicture_withValidImage_savesUploadedProfilePictureRef() {
    var file = mock(org.springframework.web.multipart.MultipartFile.class);
    UserDetailsResponse response =
        new UserDetailsResponse(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getName(),
            testUser.getBio(),
            testUser.getEmail(),
            testUser.getBirthdate(),
            "http://localhost:3900/ctrl-test-media/users/profile-pictures/avatar.png",
            Instant.now(),
            false);
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(fileStorageService.uploadProfilePicture(file))
        .thenReturn("users/profile-pictures/avatar.png");
    when(userRepository.save(testUser)).thenReturn(testUser);
    when(userMapper.toUserDetailsResponse(testUser)).thenReturn(response);

    UserDetailsResponse result = userService.updateProfilePicture(testUser, file);

    assertEquals("users/profile-pictures/avatar.png", testUser.getProfilePicture());
    assertEquals(response, result);
    verify(fileStorageService).uploadProfilePicture(file);
    verify(userRepository).save(testUser);
  }

  @Test
  void verifyUser_ShouldUpdateUser_WhenProviderReturnsValidClaims() {
    VerifyIdentityRequest request = new VerifyIdentityRequest("validCode");
    Jwt mockJwt = mock(Jwt.class);

    when(idAustriaOidcService.getTokenFromCode("validCode")).thenReturn(mockJwt);
    when(mockJwt.getClaimAsString("given_name")).thenReturn("Verified");
    when(mockJwt.getClaimAsString("family_name")).thenReturn("User");
    when(mockJwt.getClaimAsString("birthdate")).thenReturn("1985-10-10");
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userService.verifyUser(testUser, request);

    assertEquals("Verified User", testUser.getName());
    assertEquals(LocalDate.of(1985, 10, 10), testUser.getBirthdate());
    assertTrue(testUser.isVerified());
    verify(userRepository).save(testUser);
  }

  @Test
  void verifyUser_ShouldThrowException_WhenProviderCallFails() {
    VerifyIdentityRequest request = new VerifyIdentityRequest("invalidCode");
    when(idAustriaOidcService.getTokenFromCode("invalidCode"))
        .thenThrow(new RuntimeException("API down"));

    assertThrows(CommunicationException.class, () -> userService.verifyUser(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void verifyUser_ShouldThrowException_WhenBirthdateIsMissing() {
    VerifyIdentityRequest request = new VerifyIdentityRequest("validCode");
    Jwt mockJwt = mock(Jwt.class);

    when(idAustriaOidcService.getTokenFromCode("validCode")).thenReturn(mockJwt);
    when(mockJwt.getClaimAsString("given_name")).thenReturn("Verified");
    when(mockJwt.getClaimAsString("family_name")).thenReturn("User");
    when(mockJwt.getClaimAsString("birthdate")).thenReturn(null); // Missing birthdate
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    assertThrows(InvalidRequestException.class, () -> userService.verifyUser(testUser, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void verifyUser_ShouldThrowException_WhenBirthdateFormatIsInvalid() {
    VerifyIdentityRequest request = new VerifyIdentityRequest("validCode");
    Jwt mockJwt = mock(Jwt.class);

    when(idAustriaOidcService.getTokenFromCode("validCode")).thenReturn(mockJwt);
    when(mockJwt.getClaimAsString("given_name")).thenReturn("Verified");
    when(mockJwt.getClaimAsString("family_name")).thenReturn("User");
    when(mockJwt.getClaimAsString("birthdate"))
        .thenReturn("10-10-1985"); // Invalid format (expecting YYYY-MM-DD)
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    assertThrows(InvalidRequestException.class, () -> userService.verifyUser(testUser, request));
    verify(userRepository, never()).save(any());
  }

  private User createUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(username + "@test.com");
    user.setName(username);
    return user;
  }
}
