package at.ac.tuwien.inso.ctrl.chat;

import at.ac.tuwien.inso.ctrl.chat.dto.ChatRealtimePayload;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageBroadcaster {

  private final SimpMessagingTemplate messagingTemplate;

  public void publishMessageEvent(UUID chatId, ChatRealtimePayload payload) {
    messagingTemplate.convertAndSend("/topic/chats/" + chatId, payload);
  }

  public void publishChatSummary(String username, ChatResponse chatSummary) {
    messagingTemplate.convertAndSendToUser(username, "/queue/chats", chatSummary);
  }
}
