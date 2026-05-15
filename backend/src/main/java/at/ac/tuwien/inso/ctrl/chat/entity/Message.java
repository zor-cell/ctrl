package at.ac.tuwien.inso.ctrl.chat.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
@Table(name = "messages")
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_id", referencedColumnName = "chat_id", nullable = false)
  private Chat chat;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Reaction> reactions = new LinkedHashSet<>();
}
