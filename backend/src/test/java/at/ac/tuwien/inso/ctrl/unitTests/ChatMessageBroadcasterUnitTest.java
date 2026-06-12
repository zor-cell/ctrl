package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import at.ac.tuwien.inso.ctrl.chat.ChatMessageBroadcaster;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatRealtimePayload;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatRemovedPayload;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Unit tests for {@link ChatMessageBroadcaster}: ensure events are sent to the correct per-user
 * STOMP destinations with the expected payloads.
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageBroadcasterUnitTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @InjectMocks private ChatMessageBroadcaster broadcaster;

  @Test
  void publishMessageEvent_sendsToUserMessageQueue() {
    UUID chatId = UUID.randomUUID();
    ChatRealtimePayload payload = new ChatRealtimePayload("MESSAGE", chatId, message(chatId));

    broadcaster.publishMessageEvent("alice", chatId, payload);

    verify(messagingTemplate)
        .convertAndSendToUser("alice", "/queue/chats/" + chatId + "/messages", payload);
  }

  @Test
  void publishChatSummary_sendsToUserChatsQueue() {
    ChatResponse summary = chatResponse(UUID.randomUUID());

    broadcaster.publishChatSummary("bob", summary);

    verify(messagingTemplate).convertAndSendToUser("bob", "/queue/chats", summary);
  }

  @Test
  void publishChatRemoved_sendsRemovedPayloadToUserQueue() {
    UUID chatId = UUID.randomUUID();

    broadcaster.publishChatRemoved("carol", chatId);

    ArgumentCaptor<ChatRemovedPayload> captor = ArgumentCaptor.forClass(ChatRemovedPayload.class);
    verify(messagingTemplate)
        .convertAndSendToUser(
            org.mockito.ArgumentMatchers.eq("carol"),
            org.mockito.ArgumentMatchers.eq("/queue/chats/removed"),
            captor.capture());
    assertEquals(chatId, captor.getValue().chatId());
  }

  private ChatMessageResponse message(UUID chatId) {
    return new ChatMessageResponse(
        UUID.randomUUID(),
        chatId,
        null,
        "hello",
        LocalDateTime.of(2026, 4, 25, 12, 0),
        true,
        false,
        null,
        List.of(),
        List.of(),
        null);
  }

  private ChatResponse chatResponse(UUID chatId) {
    return new ChatResponse(
        chatId,
        "Bob",
        null,
        List.of(),
        null,
        LocalDateTime.of(2026, 4, 25, 12, 0),
        false,
        null,
        null,
        false,
        0,
        0);
  }
}
