package at.ac.tuwien.inso.ctrl.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "friend_requests",
    uniqueConstraints = {
      @UniqueConstraint(
          columnNames = {"sender_id", "receiver_id"},
          name = "uk_friend_requests_sender_receiver")
    })
public class FriendRequest {

  @EmbeddedId private FriendRequestId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("senderId")
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("receiverId")
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;
}
