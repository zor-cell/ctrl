package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.topic.dto.TopicCreateRequest;
import at.ac.tuwien.inso.ctrl.topic.dto.TopicResponse;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.mapper.TopicMapper;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.topic.service.impl.TopicServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
public class TopicServiceUnitTest {

  @Mock private TopicRepository topicRepository;
  @Mock private TopicMapper topicMapper;
  @Mock private EmbeddingService embeddingService;

  @InjectMocks private TopicServiceImpl topicService;

  private Topic testTopic;
  private TopicResponse testTopicResponse;
  private final float[] dummyEmbedding = new float[768];

  @BeforeEach
  void setUp() {
    String generatedId = UUID.randomUUID().toString();

    testTopic =
        Topic.builder()
            .topicId(generatedId)
            .name("Artificial Intelligence")
            .emoji("🤖")
            .embedding(dummyEmbedding)
            .build();

    testTopicResponse =
        new TopicResponse(generatedId, "Artificial Intelligence", "🤖", LocalDateTime.now());
  }

  @Test
  void getAvailableTopics_ShouldReturnSortedTopicList() {
    when(topicRepository.findAll(any(Sort.class))).thenReturn(List.of(testTopic));
    when(topicMapper.toTopicResponse(testTopic)).thenReturn(testTopicResponse);

    List<TopicResponse> result = topicService.getAvailableTopics();

    assertEquals(1, result.size());
    assertEquals("Artificial Intelligence", result.get(0).name());
    verify(topicRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
  }

  @Test
  void createTopic_ShouldCreateAndReturnTopic_WhenNameIsUnique() {
    TopicCreateRequest request =
        new TopicCreateRequest("Software Engineering", "💻", "A broad topic");
    String expectedContext = "Software Engineering: A broad topic";

    when(topicRepository.existsByName(request.name())).thenReturn(false);
    when(embeddingService.embedText(expectedContext)).thenReturn(dummyEmbedding);

    when(topicRepository.save(any(Topic.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(topicMapper.toTopicResponse(any(Topic.class))).thenReturn(testTopicResponse);

    TopicResponse result = topicService.createTopic(request);

    assertEquals(testTopicResponse, result);
    verify(embeddingService).embedText(expectedContext);
    verify(topicRepository).save(any(Topic.class));
  }

  @Test
  void createTopic_ShouldThrowException_WhenNameAlreadyExists() {
    TopicCreateRequest request = new TopicCreateRequest("Existing Topic", "⚠️", "Description");
    when(topicRepository.existsByName(request.name())).thenReturn(true);

    assertThrows(InvalidRequestException.class, () -> topicService.createTopic(request));

    verify(embeddingService, never()).embedText(anyString());
    verify(topicRepository, never()).save(any(Topic.class));
  }

  @Test
  void updateTopic_ShouldUpdateFully_WhenValidRequestWithDescription() {
    String topicId = testTopic.getTopicId();
    TopicCreateRequest request = new TopicCreateRequest("New Name", "✨", "New semantics");

    when(topicRepository.findById(topicId)).thenReturn(Optional.of(testTopic));
    when(topicRepository.findByNameIgnoreCase(request.name())).thenReturn(Optional.empty());
    when(embeddingService.embedText("New Name: New semantics")).thenReturn(dummyEmbedding);
    when(topicRepository.save(testTopic)).thenReturn(testTopic);

    TopicResponse updatedResponse =
        new TopicResponse(topicId, "New Name", "✨", LocalDateTime.now());
    when(topicMapper.toTopicResponse(testTopic)).thenReturn(updatedResponse);

    TopicResponse result = topicService.updateTopic(topicId, request);

    assertEquals("New Name", result.name());
    assertEquals("✨", result.emoji());
    verify(embeddingService).embedText(anyString());
    verify(topicRepository).save(testTopic);
  }

  @Test
  void updateTopic_ShouldUpdateWithoutEmbeddingCall_WhenDescriptionIsBlankAndNameUnchanged() {
    String topicId = testTopic.getTopicId();
    TopicCreateRequest request = new TopicCreateRequest("Artificial Intelligence", "🧠", null);

    when(topicRepository.findById(topicId)).thenReturn(Optional.of(testTopic));
    when(topicRepository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(testTopic));
    when(topicRepository.save(testTopic)).thenReturn(testTopic);
    when(topicMapper.toTopicResponse(testTopic)).thenReturn(testTopicResponse);

    topicService.updateTopic(topicId, request);

    verify(embeddingService, never()).embedText(anyString());
    verify(topicRepository).save(testTopic);
  }

  @Test
  void updateTopic_ShouldThrowException_WhenTopicDoesNotExist() {
    String unknownId = "unknown-123";
    TopicCreateRequest request = new TopicCreateRequest("Name", "😎", "Desc");

    when(topicRepository.findById(unknownId)).thenReturn(Optional.empty());

    assertThrows(InvalidRequestException.class, () -> topicService.updateTopic(unknownId, request));
    verify(topicRepository, never()).save(any());
  }

  @Test
  void updateTopic_ShouldThrowException_WhenNewNameConflictsWithAnotherTopic() {
    String currentId = testTopic.getTopicId();
    String conflictingId = "another-topic-id";
    TopicCreateRequest request = new TopicCreateRequest("Conflicting Name", "🚫", "Desc");

    Topic otherTopic = Topic.builder().topicId(conflictingId).name("Conflicting Name").build();

    when(topicRepository.findById(currentId)).thenReturn(Optional.of(testTopic));
    when(topicRepository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(otherTopic));

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> topicService.updateTopic(currentId, request));

    assertEquals("Topic name already exists", exception.getMessage());
    verify(topicRepository, never()).save(any());
  }

  @Test
  void createTopic_ShouldUseDefaultEmoji_WhenEmojiIsNull() {
    TopicCreateRequest request =
        new TopicCreateRequest("Null Emoji Topic", null, "A topic without emoji");
    String expectedContext = "Null Emoji Topic: A topic without emoji";

    when(topicRepository.existsByName(request.name())).thenReturn(false);
    when(embeddingService.embedText(expectedContext)).thenReturn(dummyEmbedding);

    when(topicRepository.save(any(Topic.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TopicResponse expectedResponse =
        new TopicResponse("dummy-id", "Null Emoji Topic", "🏷️", LocalDateTime.now());
    when(topicMapper.toTopicResponse(any(Topic.class))).thenReturn(expectedResponse);

    TopicResponse result = topicService.createTopic(request);

    assertEquals("🏷️", result.emoji());
    verify(topicRepository).save(any(Topic.class));
  }

  @Test
  void createTopic_ShouldUseDefaultEmoji_WhenEmojiIsBlank() {
    TopicCreateRequest request =
        new TopicCreateRequest("Blank Emoji Topic", "   ", "A topic with blank emoji");
    String expectedContext = "Blank Emoji Topic: A topic with blank emoji";

    when(topicRepository.existsByName(request.name())).thenReturn(false);
    when(embeddingService.embedText(expectedContext)).thenReturn(dummyEmbedding);

    when(topicRepository.save(any(Topic.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TopicResponse expectedResponse =
        new TopicResponse("dummy-id", "Blank Emoji Topic", "🏷️", LocalDateTime.now());
    when(topicMapper.toTopicResponse(any(Topic.class))).thenReturn(expectedResponse);

    TopicResponse result = topicService.createTopic(request);

    assertEquals("🏷️", result.emoji());
    verify(topicRepository).save(any(Topic.class));
  }

  @Test
  void updateTopic_ShouldNotUpdateEmbedding_WhenDescriptionIsBlank() {
    String topicId = testTopic.getTopicId();
    TopicCreateRequest request = new TopicCreateRequest("Artificial Intelligence", "🧠", "   ");

    when(topicRepository.findById(topicId)).thenReturn(Optional.of(testTopic));
    when(topicRepository.findByNameIgnoreCase(request.name())).thenReturn(Optional.of(testTopic));
    when(topicRepository.save(testTopic)).thenReturn(testTopic);
    when(topicMapper.toTopicResponse(testTopic)).thenReturn(testTopicResponse);

    topicService.updateTopic(topicId, request);

    verify(embeddingService, never()).embedText(anyString());
    verify(topicRepository).save(testTopic);
  }
}
