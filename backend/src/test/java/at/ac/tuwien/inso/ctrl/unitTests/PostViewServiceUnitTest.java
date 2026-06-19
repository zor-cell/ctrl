package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.post.dto.request.PostViewBatchRequest;
import at.ac.tuwien.inso.ctrl.post.dto.request.PostViewRequest;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.post.service.impl.PostViewServiceImpl;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.session.services.SessionService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class PostViewServiceUnitTest {

    @Mock private SeenPostRepository seenPostRepository;
    @Mock private UserRepository userRepository;
    @Mock private SessionService sessionService;
    @Mock private SessionRepository sessionRepository;

    @InjectMocks private PostViewServiceImpl postViewService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
    }

    /** recordViews – unknown user */
    @Test
    void recordViews_withUnknownUsername_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        PostViewBatchRequest request = new PostViewBatchRequest(List.of());

        assertThrows(
                UsernameNotFoundException.class,
                () -> postViewService.recordViews(request, "unknown"));

        verify(seenPostRepository, never()).upsertView(any(), any(), anyLong());
        verify(sessionService, never()).getOrCreateLastSession(any());
        verify(sessionRepository, never()).save(any());
    }

    /** recordViews – empty views list */
    @Test
    void recordViews_withEmptyViewsList_shouldNotInteractWithSessionAtAll() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        PostViewBatchRequest request = new PostViewBatchRequest(List.of());

        postViewService.recordViews(request, "alice");

        verify(seenPostRepository, never()).upsertView(any(), any(), anyLong());
        verify(sessionService, never()).getOrCreateLastSession(any());
        verify(sessionRepository, never()).save(any());
    }

    /** recordViews – upsertView is called for every entry */
    @ParameterizedTest
    @MethodSource("inputArrays")
    void recordViews_withViews_shouldCallUpsertViewForEachEntry(
            List<PostViewRequest> views, int expectedUpsertCalls) {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(0);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request = new PostViewBatchRequest(views);

        postViewService.recordViews(request, "alice");

        verify(seenPostRepository, times(expectedUpsertCalls)).upsertView(any(), any(), anyLong());
    }

    static Stream<Arguments> inputArrays() {
        return Stream.of(
                Arguments.of(List.of(entry("post-1", 1000L)), 1),
                Arguments.of(List.of(entry("post-1", 1000L), entry("post-2", 2000L)), 2),
                Arguments.of(List.of(entry("post-1", 500L), entry("post-2", 1500L), entry("post-3", 3000L)), 3));
    }

    /** recordViews – correct postId and durationMs are forwarded to upsertView */
    @Test
    void recordViews_withViews_shouldForwardCorrectPostIdAndDurationToUpsertView() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(0);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request =
                new PostViewBatchRequest(List.of(entry("post-42", 7500L)));

        postViewService.recordViews(request, "alice");

        verify(seenPostRepository).upsertView(user.getId(), "post-42", 7500L);
    }

    /** recordViews – session postsViewed is incremented correctly */
    @ParameterizedTest
    @MethodSource("inputArrays")
    void recordViews_withViews_shouldIncrementPostsViewedByNumberOfViews(
            List<PostViewRequest> views, int expectedUpsertCalls) {
        int initialPostsViewed = 5;
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(initialPostsViewed);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request = new PostViewBatchRequest(views);

        postViewService.recordViews(request, "alice");

        assertEquals(initialPostsViewed + views.size(), session.getPostsViewed());
    }

    @Test
    void recordViews_withViews_shouldStartCountFromZeroWhenSessionIsNew() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(0);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request =
                new PostViewBatchRequest(List.of(entry("post-1", 1000L), entry("post-2", 2000L)));

        postViewService.recordViews(request, "alice");

        assertEquals(2, session.getPostsViewed());
    }

    /** recordViews – session is persisted */
    @Test
    void recordViews_withViews_shouldSaveUpdatedSession() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(3);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request =
                new PostViewBatchRequest(List.of(entry("post-1", 1000L)));

        postViewService.recordViews(request, "alice");

        ArgumentCaptor<SessionHistory> captor = ArgumentCaptor.forClass(SessionHistory.class);
        verify(sessionRepository).save(captor.capture());
        assertEquals(session, captor.getValue());
    }

    @Test
    void recordViews_withEmptyViewsList_shouldNeverSaveSession() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        PostViewBatchRequest request = new PostViewBatchRequest(List.of());

        postViewService.recordViews(request, "alice");

        verify(sessionRepository, never()).save(any());
    }

    /** recordViews – user resolution */
    @Test
    void recordViews_withViews_shouldLookUpUserByUsername() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(0);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request =
                new PostViewBatchRequest(List.of(entry("post-1", 1000L)));

        postViewService.recordViews(request, "alice");

        verify(userRepository).findByUsername("alice");
    }

    @Test
    void recordViews_withViews_shouldPassResolvedUserToSessionService() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        SessionHistory session = buildSession(0);
        when(sessionService.getOrCreateLastSession(user)).thenReturn(session);

        PostViewBatchRequest request =
                new PostViewBatchRequest(List.of(entry("post-1", 1000L)));

        postViewService.recordViews(request, "alice");

        verify(sessionService).getOrCreateLastSession(user);
    }

    /** Helpers */
    private static PostViewRequest entry(String postId, long durationMs) {
        return new PostViewRequest(postId, durationMs);
    }

    private SessionHistory buildSession(int postsViewed) {
        SessionHistory session = new SessionHistory();
        session.setPostsViewed(postsViewed);
        return session;
    }
}