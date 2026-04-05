package at.ac.tuwien.inso.ctrl.chat.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatMemberId implements Serializable {
    private String chatId;
    private String username;
}