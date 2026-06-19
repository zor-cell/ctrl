package at.ac.tuwien.inso.ctrl.user.config;

import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMemberId;
import at.ac.tuwien.inso.ctrl.chat.entity.Message;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.MessageRepository;
import at.ac.tuwien.inso.ctrl.user.dto.Role;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!test")
public class UserSeederConfig {
  @Value("${application.seed.admin.username}")
  private String adminUsername;

  @Value("${application.seed.admin.password}")
  private String adminPassword;

  @Bean
  CommandLineRunner backfillUserVerification(
      UserRepository userRepository, TransactionTemplate transactionTemplate) {
    return args ->
        transactionTemplate.executeWithoutResult(
            transactionStatus -> userRepository.backfillNullVerifiedUsers());
  }

  @Bean
  CommandLineRunner seedAdminAndTestUsers(
      UserRepository userRepository,
      FriendRelationRepository friendRelationRepository,
      ChatRepository chatRepository,
      MessageRepository messageRepository,
      PasswordEncoder passwordEncoder,
      TransactionTemplate transactionTemplate) {
    return args ->
        transactionTemplate.executeWithoutResult(
            transactionStatus -> {

              // create admin user
              if (!userRepository.existsByUsername(adminUsername)) {
                userRepository.save(
                    User.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .name("System Administrator")
                        .email("admin@system.local")
                        .birthdate(LocalDate.of(2000, 4, 23))
                        .isVerified(true)
                        .emailVerified(true)
                        .role(Role.ADMIN)
                        .build());
              }

              // create e2e test user
              userRepository.save(
                  User.builder()
                      .username("TestUser")
                      .password(passwordEncoder.encode("test12345678"))
                      .name("E2E Test User")
                      .email("test@system.local")
                      .birthdate(LocalDate.of(2000, 4, 23))
                      .isVerified(true)
                      .emailVerified(true)
                      .role(Role.TEST)
                      .build());

              // create test users
              List<User> chatUsers =
                  List.of(
                      ensureSeedUser(
                          userRepository,
                          passwordEncoder,
                          "test",
                          "test",
                          "Alice Test",
                          "alice@test.local",
                          LocalDate.of(1995, 2, 12),
                          true),
                      ensureSeedUser(
                          userRepository,
                          passwordEncoder,
                          "test1",
                          "test1",
                          "Bob Test",
                          "bob@test.local",
                          LocalDate.of(2003, 7, 16),
                          true),
                      ensureSeedUser(
                          userRepository,
                          passwordEncoder,
                          "Philippe",
                          "Philippe",
                          "Philippe",
                          "philippe@test.local",
                          LocalDate.of(1997, 5, 4),
                          true),
                      ensureSeedUser(
                          userRepository,
                          passwordEncoder,
                          "Felix",
                          "Felix",
                          "Felix",
                          "felix@test.local",
                          LocalDate.of(1998, 9, 18),
                          true),
                      ensureSeedUser(
                          userRepository,
                          passwordEncoder,
                          "Franz",
                          "Franz",
                          "Franz",
                          "franz@test.local",
                          LocalDate.of(1996, 11, 21),
                          true),
                      ensureSeedUser(
                          userRepository,
                          passwordEncoder,
                          "Chris",
                          "Chris",
                          "Chris",
                          "chris@test.local",
                          LocalDate.of(1999, 1, 9),
                          true));

              seedFriendships(friendRelationRepository, chatUsers);
              seedChat(chatRepository, messageRepository, chatUsers);
            });
  }

  private User ensureSeedUser(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      String username,
      String password,
      String name,
      String email,
      LocalDate birthdate,
      boolean verified) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseGet(
                () ->
                    User.builder().username(username).birthdate(birthdate).role(Role.USER).build());

    user.setPassword(passwordEncoder.encode(password));
    user.setName(name);
    user.setEmail(email);
    user.setBirthdate(birthdate);
    user.setVerified(verified);
    user.setEmailVerified(true);
    user.setRole(Role.USER);
    return userRepository.save(user);
  }

  private void seedFriendships(
      FriendRelationRepository friendRelationRepository, List<User> users) {
    for (int firstIndex = 0; firstIndex < users.size(); firstIndex++) {
      for (int secondIndex = firstIndex + 1; secondIndex < users.size(); secondIndex++) {
        User firstUser = users.get(firstIndex);
        User secondUser = users.get(secondIndex);
        if (friendRelationRepository.existsByUsers(firstUser, secondUser)) {
          continue;
        }

        friendRelationRepository.save(
            FriendRelation.builder()
                .id(new FriendRelationId(firstUser.getId(), secondUser.getId()))
                .friend1(firstUser)
                .friend2(secondUser)
                .since(LocalDateTime.now())
                .build());
      }
    }
  }

  private void seedChat(
      ChatRepository chatRepository, MessageRepository messageRepository, List<User> users) {
    String seedChatName = "ASEPR Test Chat";
    Chat chat =
        chatRepository.findChatsForUser(users.getFirst()).stream()
            .filter(existingChat -> seedChatName.equals(existingChat.getName()))
            .findFirst()
            .orElseGet(
                () -> {
                  Chat newChat =
                      Chat.builder()
                          .chatId(UUID.randomUUID())
                          .name(seedChatName)
                          .createdAt(LocalDateTime.now())
                          .owner(users.getFirst())
                          .members(new ArrayList<>())
                          .build();

                  for (User user : users) {
                    newChat
                        .getMembers()
                        .add(
                            ChatMember.builder()
                                .id(new ChatMemberId(newChat.getChatId(), user.getId()))
                                .chat(newChat)
                                .user(user)
                                .joinedAt(LocalDateTime.now())
                                .build());
                  }

                  return chatRepository.save(newChat);
                });

    if (chat.getOwner() == null) {
      chat.setOwner(users.getFirst());
      chat = chatRepository.save(chat);
    }

    if (messageRepository.findTopByChatOrderByDateDesc(chat).isEmpty()) {
      messageRepository.save(
          Message.builder()
              .messageId(UUID.randomUUID())
              .chat(chat)
              .fromUser(users.getFirst())
              .content("Hi zusammen, das ist der vorbereitete Test-Chat.")
              .date(LocalDateTime.now().minusMinutes(2))
              .build());
      messageRepository.save(
          Message.builder()
              .messageId(UUID.randomUUID())
              .chat(chat)
              .fromUser(users.get(3))
              .content("Perfekt, dann kann man Chat-Nachrichten direkt ausprobieren.")
              .date(LocalDateTime.now().minusMinutes(1))
              .build());
    }
  }
}
