package at.ac.tuwien.inso.ctrl.chat.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "messages",
    indexes = {
      @Index(name = "idx_messages_chat_date", columnList = "chat_id, date"),
      @Index(
          name = "idx_messages_chat_from_system_date",
          columnList = "chat_id, from_user_id, system, date")
    })
public class Message {
  @Id
  @Column(name = "message_id", nullable = false, unique = true)
  private UUID messageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_user_id", referencedColumnName = "id", nullable = false)
  private User fromUser;

  @Column(nullable = false, length = 4000)
  private String content;

  @Column(name = "date")
  private LocalDateTime date;

  @Column(nullable = false)
  @Builder.Default
  private boolean system = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "system_event")
  private SystemEventType systemEvent;

  @Column(name = "system_target_user_ids")
  private String systemTargetUserIds;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_id", referencedColumnName = "chat_id", nullable = false)
  private Chat chat;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shared_id", referencedColumnName = "shared_id")
  private Shared shared;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Reaction> reactions = new LinkedHashSet<>();
}
