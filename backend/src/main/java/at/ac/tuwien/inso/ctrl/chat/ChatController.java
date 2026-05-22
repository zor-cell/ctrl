package at.ac.tuwien.inso.ctrl.chat;

import at.ac.tuwien.inso.ctrl.chat.dto.AddChatMembersRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.AddReactionRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.CreateChatRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.SendMessageRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @GetMapping
  public List<ChatResponse> getChats(@AuthenticationPrincipal User currentUser) {
    return chatService.getChats(currentUser);
  }

  @GetMapping("/{chatId}")
  public ChatResponse getChat(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID chatId) {
    return chatService.getChat(currentUser, chatId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ChatResponse createChat(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody CreateChatRequest request) {
    return chatService.createChat(currentUser, request);
  }

  @PostMapping(value = "/{chatId}/group-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ChatResponse uploadGroupImage(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID chatId,
      @RequestPart("file") MultipartFile file) {
    return chatService.uploadGroupImage(currentUser, chatId, file);
  }

  @GetMapping("/{chatId}/messages")
  public List<ChatMessageResponse> getMessages(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID chatId) {
    return chatService.getMessages(currentUser, chatId);
  }

  @PostMapping("/{chatId}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public ChatMessageResponse sendMessage(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID chatId,
      @Valid @RequestBody SendMessageRequest request) {
    return chatService.sendMessage(currentUser, chatId, request);
  }

  @PostMapping("/{chatId}/messages/{messageId}/reactions")
  public ChatMessageResponse reactToMessage(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID chatId,
      @PathVariable UUID messageId,
      @Valid @RequestBody AddReactionRequest request) {
    return chatService.reactToMessage(currentUser, chatId, messageId, request);
  }

  @DeleteMapping("/{chatId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteChat(@AuthenticationPrincipal User currentUser, @PathVariable UUID chatId) {
    chatService.deleteChat(currentUser, chatId);
  }

  @PostMapping("/{chatId}/members")
  public ChatResponse addMembers(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID chatId,
      @Valid @RequestBody AddChatMembersRequest request) {
    return chatService.addMembers(currentUser, chatId, request);
  }

  @DeleteMapping("/{chatId}/members/{memberId}")
  public ChatResponse removeMember(
      @AuthenticationPrincipal User currentUser,
      @PathVariable UUID chatId,
      @PathVariable UUID memberId) {
    return chatService.removeMember(currentUser, chatId, memberId);
  }

  @DeleteMapping("/{chatId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leaveChat(@AuthenticationPrincipal User currentUser, @PathVariable UUID chatId) {
    chatService.leaveChat(currentUser, chatId);
  }
}
