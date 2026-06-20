package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestAction;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.FriendshipService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FriendshipServiceUnitTest {

  @Mock private FriendRequestRepository friendRequestRepository;
  @Mock private FriendRelationRepository friendRelationRepository;
  @Mock private FollowRelationRepository followRelationRepository;
  @Mock private UserRepository userRepository;
  @Mock private RelationshipMapper socialMapper;

  @InjectMocks private FriendshipService friendshipService;

  @Test
  public void handleIncomingRequestAction_friendRequestNotForUser_throwsInvalidRequestException() {
    // Given
    User currentUser = User.builder().id(UUID.randomUUID()).username("current").build();
    User sender = User.builder().id(UUID.randomUUID()).username("sender").build();
    User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();

    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(sender.getId(), otherUser.getId()))
            .sender(sender)
            .receiver(otherUser)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();

    when(userRepository.findByUsername(sender.getUsername())).thenReturn(Optional.of(sender));
    when(friendRequestRepository.findBySenderAndReceiver(sender, currentUser))
        .thenReturn(Optional.of(request));

    // When / Then
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                friendshipService.handleIncomingRequestAction(
                    currentUser, sender.getUsername(), FriendRequestAction.ACCEPT));

    assertEquals(
        "You cannot manage this friend request, since you are not the receiver",
        exception.getMessage());
  }

  @Test
  public void cancelSentRequest_notSentByUser_throwsInvalidRequestException() {
    // Given
    User currentUser = User.builder().id(UUID.randomUUID()).username("current").build();
    User receiver = User.builder().id(UUID.randomUUID()).username("receiver").build();
    User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();

    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(otherUser.getId(), receiver.getId()))
            .sender(otherUser)
            .receiver(receiver)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();

    when(userRepository.findByUsername(receiver.getUsername())).thenReturn(Optional.of(receiver));
    when(friendRequestRepository.findBySenderAndReceiver(currentUser, receiver))
        .thenReturn(Optional.of(request));

    // When / Then
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> friendshipService.cancelSentRequest(currentUser, receiver.getUsername()));

    assertEquals(
        "You cannot cancel this friend request, since you are not the sender",
        exception.getMessage());
  }
}
