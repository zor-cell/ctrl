package at.ac.tuwien.inso.ctrl.chat.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chats")
public class Chat {
  @Id
  @Column(name = "chat_id", nullable = false, unique = true)
  private UUID chatId;

  private String name;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", referencedColumnName = "id")
  private User owner;

  @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ChatMember> members = new ArrayList<>();

  @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Message> messages = new LinkedHashSet<>();
}
