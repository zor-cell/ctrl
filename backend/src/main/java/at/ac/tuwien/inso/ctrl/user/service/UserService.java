package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.auth.service.EmailVerificationService;
import at.ac.tuwien.inso.ctrl.chat.ChatService;
import at.ac.tuwien.inso.ctrl.chat.repository.ReactionRepository;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.BusinessVerificationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserMapper userMapper;
  private final UserRepository userRepository;
  private final FriendRequestRepository friendRequestRepository;
  private final FriendRelationRepository friendRelationRepository;
  private final FollowRelationRepository followRelationRepository;
  private final BusinessVerificationRepository businessVerificationRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final IdAustriaOidcService idAustriaOidcService;
  private final FileStorageService fileStorageService;
  private final BlockService blockService;
  private final FollowService followService;
  private final EmailVerificationService emailVerificationService;
  private final EmailVerificationRepository emailVerificationRepository;
  private final PasswordResetRepository passwordResetRepository;
  private final ReportService reportService;
  private final PostRepository postRepository;
  private final PostVoteRepository postVoteRepository;
  private final SourceRepository sourceRepository;
  private final SourceVoteRepository sourceVoteRepository;
  private final CommentRepository commentRepository;
  private final ReactionRepository reactionRepository;
  private final SeenPostRepository seenPostRepository;
  private final AlgorithmRepository algorithmRepository;
  private final ChatService chatService;

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public UserDetailsResponse getUser(UUID userId) {
    User user = getUserInternal(userId);
    return userMapper.toUserDetailsResponse(user);
  }

  public PublicUserProfileResponse getPublicProfile(String username, User currentUser) {

    User user =
        userRepository
            .findByUsernameAndDeletedFalse(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (user.isSuspended()) {
      throw new UsernameNotFoundException("User not found");
    }

    if (blockService.inBlockedRelationship(currentUser, user)) {
      throw new UsernameNotFoundException("User not found");
    }

    boolean canSeePrivatePosts =
        currentUser != null
            && (user.getId().equals(currentUser.getId())
                || friendRelationRepository.existsByUsers(user, currentUser)
                || currentUser.getRole() == Role.ADMIN);
    boolean wasReportedByUser =
        reportService.wasReportedByUser(
            currentUser, ContentTypeEnum.ACCOUNT, String.valueOf(user.getId()));
    return userMapper.toPublicUserProfileResponse(user, canSeePrivatePosts, wasReportedByUser);
  }

  @Transactional
  public UserDetailsResponse updateUser(User currentUser, UpdateProfileRequest request) {
    User user = getUserInternal(currentUser.getId());

    String newEmail = request.email().trim();
    if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
      throw new ResourceAlreadyExistsException("User", "email", newEmail);
    }

    boolean emailChanged = false;
    if (!user.getEmail().equalsIgnoreCase(newEmail)) {
      emailChanged = true;
    }

    String newName = request.name().trim();
    if (user.isVerified()) {
      if (!user.getName().equalsIgnoreCase(newName)) {
        throw new InvalidRequestException("Verified users cannot change their name");
      }

      if (!Objects.equals(user.getBirthdate(), request.birthdate())) {
        throw new InvalidRequestException("Verified users cannot change their birthdate");
      }
    }

    user.setName(newName);
    user.setEmail(newEmail);
    user.setBio(request.bio() == null ? null : request.bio().trim());
    user.setBirthdate(request.birthdate());
    if (emailChanged) {
      if (requireEmailVerification) {
        user.setEmailVerified(false);
      }
    }

    User updatedUser = userRepository.save(user);
    if (emailChanged) {
      if (requireEmailVerification) {
        SendEmailRequest emailRequest =
            new SendEmailRequest(updatedUser.getUsername(), updatedUser.getEmail());
        emailVerificationService.sendVerificationEmail(emailRequest);
      }
    }

    return userMapper.toUserDetailsResponse(updatedUser);
  }

  @Transactional
  public UserDetailsResponse updateProfilePicture(User currentUser, MultipartFile file) {
    User user = getUserInternal(currentUser.getId());
    String previousProfilePictureRef = user.getProfilePicture();
    String profilePictureRef = fileStorageService.uploadProfilePicture(file);
    user.setProfilePicture(profilePictureRef);

    User updatedUser = userRepository.save(user);
    fileStorageService.deleteProfilePicture(previousProfilePictureRef);
    return userMapper.toUserDetailsResponse(updatedUser);
  }

  @Transactional
  public void deleteCurrentUser(User currentUser) {
    User user = getUserInternal(currentUser.getId());
    if (user.isDeleted()) {
      return;
    }

    UUID userId = user.getId();
    String userIdString = userId.toString();
    String previousProfilePictureRef = user.getProfilePicture();

    removePostVotes(userIdString);
    removeSourceVotes(userIdString);
    reactionRepository.deleteAllByUserId(userId);
    deleteCommentsWithReplies(userId);
    postRepository.markAllByUserAsRemoved(userId);
    seenPostRepository.deleteAllByUserId(userId);
    friendRequestRepository.deleteAllByUser(user);
    friendRelationRepository.deleteAllByUser(user);
    followRelationRepository.deleteAllByUser(user);
    passwordResetRepository.deleteByUserId(userId);
    emailVerificationRepository.deleteByUser_Id(userId);
    businessVerificationRepository.deleteByBusinessUser(user);
    algorithmRepository.deleteByUserId(userId);

    user.setUsername("deleted-user-" + userId);
    user.setEmail("deleted-" + userId + "@deleted.local");
    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setName("Deleted user");
    user.setBio(null);
    user.setBirthdate(null);
    user.setProfilePicture(null);
    user.setInteractionPreferenceEmbedding(null);
    user.setVerified(false);
    user.setEmailVerified(false);
    user.setSuspended(true);
    user.setDeleted(true);

    userRepository.save(user);
    chatService.createAccountDeletedSystemMessages(user);
    fileStorageService.deleteProfilePicture(previousProfilePictureRef);
  }

  @Transactional
  public UserDetailsResponse deleteProfilePicture(User currentUser) {
    User user = getUserInternal(currentUser.getId());
    String previousProfilePictureRef = user.getProfilePicture();
    user.setProfilePicture(null);

    User updatedUser = userRepository.save(user);
    fileStorageService.deleteProfilePicture(previousProfilePictureRef);
    return userMapper.toUserDetailsResponse(updatedUser);
  }

  @Transactional
  public void updatePassword(User currentUser, UpdatePasswordRequest request) {
    User user = getUserInternal(currentUser.getId());

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new InvalidPasswordException("Current password is incorrect");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));

    userRepository.save(user);
  }

  @Transactional
  public AuthResponse updateUsername(User currentUser, UpdateUsernameRequest request) {
    String newUsername = request.newUsername().trim();

    if (userRepository.existsByUsername(newUsername)) {
      throw new ResourceAlreadyExistsException("User", "username", newUsername);
    }

    User user = getUserInternal(currentUser.getId());
    user.setUsername(newUsername);
    userRepository.save(user);

    String newToken = jwtService.generateToken(user);
    return new AuthResponse(newToken);
  }

  @Transactional
  public UserDetailsResponse verifyUser(User currentUser, VerifyIdentityRequest request) {
    Jwt jwt;
    try {
      jwt =
          request.redirectUri() == null || request.redirectUri().isBlank()
              ? idAustriaOidcService.getTokenFromCode(request.code())
              : idAustriaOidcService.getTokenFromCode(request.code(), request.redirectUri());
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new CommunicationException("Failed to verify identity with external provider");
    }

    User user = getUserInternal(currentUser.getId());

    String firstName = jwt.getClaimAsString("given_name");
    String lastName = jwt.getClaimAsString("family_name");
    if (firstName != null && lastName != null) {
      user.setName(firstName + " " + lastName);
    }

    String birthdateStr = jwt.getClaimAsString("birthdate");
    if (birthdateStr == null || birthdateStr.isBlank()) {
      throw new InvalidRequestException(
          "Verification failed: No birthdate provided by external provider");
    }

    try {
      user.setBirthdate(LocalDate.parse(birthdateStr));
    } catch (DateTimeParseException e) {
      throw new InvalidRequestException(
          "Verification failed: Invalid birthdate format received from external provider");
    }

    user.setVerified(true);

    User updatedUser = userRepository.save(user);
    return userMapper.toUserDetailsResponse(updatedUser);
  }

  private User getUserInternal(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  private void removePostVotes(String userId) {
    List<PostVote> votes = postVoteRepository.findByIdUserId(userId);
    for (PostVote vote : votes) {
      Post post = vote.getPost();
      if ("up".equals(vote.getVoteType())) {
        post.setUpvotes(decrementCounter(post.getUpvotes()));
      } else if ("down".equals(vote.getVoteType())) {
        post.setDownvotes(decrementCounter(post.getDownvotes()));
      }
      postRepository.save(post);
    }
    postVoteRepository.deleteAll(votes);
  }

  private void removeSourceVotes(String userId) {
    List<SourceVote> votes = sourceVoteRepository.findByIdUserId(userId);
    for (SourceVote vote : votes) {
      Source source = vote.getSource();
      if ("up".equals(vote.getVoteType())) {
        source.setUpvotes(decrementCounter(source.getUpvotes()));
      } else if ("down".equals(vote.getVoteType())) {
        source.setDownvotes(decrementCounter(source.getDownvotes()));
      }
      sourceRepository.save(source);
    }
    sourceVoteRepository.deleteAll(votes);
  }

  private void deleteCommentsWithReplies(UUID userId) {
    List<Comment> comments = commentRepository.findAll();
    Set<String> commentIdsToDelete = new HashSet<>();
    for (Comment comment : comments) {
      if (comment.getUser().getId().equals(userId)) {
        commentIdsToDelete.add(comment.getCommentId());
      }
    }

    boolean changed;
    do {
      changed = false;
      for (Comment comment : comments) {
        Comment parent = comment.getParentComment();
        if (parent != null && commentIdsToDelete.contains(parent.getCommentId())) {
          changed |= commentIdsToDelete.add(comment.getCommentId());
        }
      }
    } while (changed);

    comments.stream()
        .filter(comment -> commentIdsToDelete.contains(comment.getCommentId()))
        .sorted(Comparator.comparingInt(this::commentDepth).reversed())
        .forEach(commentRepository::delete);
  }

  private int commentDepth(Comment comment) {
    int depth = 0;
    Comment parent = comment.getParentComment();
    Set<String> visitedParentIds = new HashSet<>();
    while (parent != null && visitedParentIds.add(parent.getCommentId())) {
      depth++;
      parent = parent.getParentComment();
    }
    return depth;
  }

  private int decrementCounter(Integer value) {
    return Math.max(0, (value == null ? 0 : value) - 1);
  }

  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void conductUserCleanup() {
    List<User> users = userRepository.findEmailUnverifiedUsersOlderThanOneDay();
    for (User user : users) {
      emailVerificationRepository.deleteByUser_Id(user.getId());
      passwordResetRepository.deleteUnusedTokensByUserId(user.getId());
    }

    userRepository.deleteUnverifiedUsersOlderThanOneDay();
  }
}
