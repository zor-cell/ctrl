package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.common.vote.VoteManager;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.mapper.SourceMapper;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.source.service.impl.SourceServiceImpl;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class SourceUnitTest {

  @Mock private SourceRepository sourceRepository;
  @Mock private SourceMapper sourceMapper;
  @Mock private SourceVoteRepository sourceVoteRepository;
  @Mock private UserRepository userRepository;
  @Mock private VoteManager voteManager;

  @InjectMocks private SourceServiceImpl sourceService;

  /** createSource - input validation * */

  // createSource does not throw on null, it returns null explicitly
  @Test
  void createSource_shouldReturnNull_whenRequestIsNull() {
    Source result = sourceService.createSource(null);

    assertNull(result);
    verifyNoInteractions(sourceMapper, sourceRepository);
  }

  @ParameterizedTest
  @MethodSource("validCreateSourceInputs")
  void createSource_shouldNotThrow_whenInputIsValid(CreateSourceRequest request) {
    when(sourceMapper.toEntity(request)).thenReturn(buildSource("source-1"));
    when(sourceRepository.save(any())).thenReturn(buildSource("source-1"));

    assertDoesNotThrow(() -> sourceService.createSource(request));
  }

  static Stream<Arguments> validCreateSourceInputs() {
    return Stream.of(
        Arguments.of(mock(CreateSourceRequest.class)), // minimal mock
        Arguments.of(mock(CreateSourceRequest.class)) // second valid request
        );
  }

  /** voteSource - input validation * */
  @ParameterizedTest
  @MethodSource("invalidVoteSourceInputs")
  void voteSource_shouldThrow_whenInputIsInvalid(
      String sourceId,
      String username,
      String voteType,
      Class<? extends Exception> expectedException) {
    // source not found
    if (expectedException == EntityNotFoundException.class) {
      when(sourceRepository.findById(sourceId)).thenReturn(Optional.empty());
    }

    // source found but user not found
    if (expectedException == UsernameNotFoundException.class) {
      when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(buildSource(sourceId)));
      when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
    }

    if (expectedException == IllegalArgumentException.class) {
      lenient()
          .when(sourceRepository.findById(sourceId))
          .thenReturn(Optional.of(buildSource(sourceId)));
      lenient()
          .when(userRepository.findByUsername(username))
          .thenReturn(Optional.of(buildUser(username)));
      lenient()
          .when(sourceVoteRepository.findById(any(SourceVoteId.class)))
          .thenReturn(Optional.empty());
    }

    VoteRequest request = mock(VoteRequest.class);
    lenient().when(request.voteType()).thenReturn(voteType);

    assertThrows(expectedException, () -> sourceService.voteSource(sourceId, request, username));
  }

  static Stream<Arguments> invalidVoteSourceInputs() {
    return Stream.of(
        Arguments.of(
            "unknown-source", "alice", "up", EntityNotFoundException.class), // source not found
        Arguments.of("source-1", "ghost", "up", UsernameNotFoundException.class) // user not found
        );
  }

  @ParameterizedTest
  @MethodSource("validVoteSourceInputs")
  void voteSource_shouldNotThrow_whenInputIsValid(
      String sourceId, String username, String voteType) {
    Source source = buildSource(sourceId);
    User user = buildUser(username);
    VoteRequest request = mock(VoteRequest.class);

    when(request.voteType()).thenReturn(voteType);
    when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(sourceVoteRepository.findById(any(SourceVoteId.class))).thenReturn(Optional.empty());
    when(voteManager.processVote(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(mock(VoteResponse.class));

    assertDoesNotThrow(() -> sourceService.voteSource(sourceId, request, username));
  }

  static Stream<Arguments> validVoteSourceInputs() {
    return Stream.of(
        Arguments.of("source-1", "alice", "up"), Arguments.of("source-2", "bob", "down"));
  }

  /** voteSource - behavior * */
  @Test
  void voteSource_withNoExistingVote_shouldDelegateToVoteManager() {
    String sourceId = "source-1";
    String username = "alice";
    Source source = buildSource(sourceId);
    User user = buildUser(username);
    VoteRequest request = mock(VoteRequest.class);
    VoteResponse expectedResponse = mock(VoteResponse.class);

    when(request.voteType()).thenReturn("up");
    when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(sourceVoteRepository.findById(any(SourceVoteId.class))).thenReturn(Optional.empty());
    when(voteManager.processVote(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(expectedResponse);

    VoteResponse result = sourceService.voteSource(sourceId, request, username);

    assertEquals(expectedResponse, result);
    verify(voteManager).processVote(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void voteSource_withExistingVote_shouldPassExistingVoteToVoteManager() {
    String sourceId = "source-1";
    String username = "alice";
    Source source = buildSource(sourceId);
    User user = buildUser(username);
    SourceVoteId voteId = new SourceVoteId(sourceId, user.getId().toString());
    SourceVote existingVote = SourceVote.builder().id(voteId).source(source).voteType("up").build();
    VoteRequest request = mock(VoteRequest.class);

    when(request.voteType()).thenReturn("down");
    when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(sourceVoteRepository.findById(any(SourceVoteId.class)))
        .thenReturn(Optional.of(existingVote));
    when(voteManager.processVote(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(mock(VoteResponse.class));

    sourceService.voteSource(sourceId, request, username);

    verify(voteManager)
        .processVote(
            eq(source), eq(Optional.of(existingVote)), eq("down"), any(), any(), any(), any());
  }

  /** Helpers * */
  private Source buildSource(String sourceId) {
    Source source = new Source();
    source.setSourceId(sourceId);
    return source;
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }
}
