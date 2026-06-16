package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.service.AlgorithmService;
import at.ac.tuwien.inso.ctrl.algorithm.service.RecommendationService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceUnitTest {

    @Mock private AlgorithmService algorithmService;
    @Mock private PostRepository postRepository;

    @InjectMocks private RecommendationService recommendationService;

    private User testUser;
    private AlgorithmConfig testConfig;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setInteractionPreferenceEmbedding(new float[]{0.1f, 0.2f});

        Topic tech = new Topic();
        tech.setTopicId("tech");

        testConfig = AlgorithmConfig.builder()
                .user(testUser)
                .weightInteraction(80)
                .weightTopic(50)
                .weightRecency(90)
                .weightPopularity(40)
                .weightSourceQuality(60)
                .weightExploration(30)
                .topics(Set.of(tech))
                .topicPreferenceEmbedding(new float[]{0.5f, 0.6f})
                .build();
    }

    @Test
    void getRecommendedPosts_PrivateFeed_WithNewPostsAvailable_DoesNotCallOldPostsFallback() {
        when(algorithmService.getOrCreateConfig(testUser)).thenReturn(testConfig);

        List<String> newCandidates = List.of("new_post_1", "new_post_2");
        when(postRepository.getPrivatePostCandidatePool(eq(testUser.getId()), any(), any(), eq(50), eq(false)))
                .thenReturn(newCandidates);

        when(postRepository.getRankedPosts(any(), any(), anyDouble(), any(), anyDouble(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of());

        recommendationService.getRecommendedPosts(testUser, 0, 10, false);

        verify(postRepository).getPrivatePostCandidatePool(eq(testUser.getId()), any(), any(), eq(50), eq(false));
        verify(postRepository, never()).getPrivatePostCandidatePool(any(), any(), any(), anyInt(), eq(true));
    }

    @Test
    void getRecommendedPosts_CalculatesCorrectPaginationOffsetAndLimit() {
        int page = 3;
        int size = 20;
        int expectedOffset = 60;

        when(algorithmService.getOrCreateConfig(testUser)).thenReturn(testConfig);
        when(postRepository.getPublicPostCandidatePool(any(), any(), any(), anyInt()))
                .thenReturn(List.of("post1"));
        when(postRepository.getRankedPosts(any(), any(), anyDouble(), any(), anyDouble(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of());

        recommendationService.getRecommendedPosts(testUser, page, size, true);

        verify(postRepository).getRankedPosts(
                any(), any(), anyDouble(), any(), anyDouble(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                eq(size),
                eq(expectedOffset)
        );
    }

    @Test
    void getRecommendedPosts_ConvertsWeightsAndMapsFullExplanationDto() {
        when(algorithmService.getOrCreateConfig(testUser)).thenReturn(testConfig);
        when(postRepository.getPublicPostCandidatePool(any(), any(), any(), anyInt()))
                .thenReturn(List.of("post_123"));

        PostScoreProjection mockProjection = mock(PostScoreProjection.class);
        when(mockProjection.getPostId()).thenReturn("post_123");
        when(mockProjection.getScoreInteraction()).thenReturn(0.85);
        when(mockProjection.getScoreTopic()).thenReturn(0.70);
        when(mockProjection.getScoreRecency()).thenReturn(0.95);
        when(mockProjection.getScoreTrending()).thenReturn(0.40);
        when(mockProjection.getScoreSource()).thenReturn(0.60);
        when(mockProjection.getScoreExploration()).thenReturn(0.10);
        when(mockProjection.getTotalScore()).thenReturn(7.85);

        when(postRepository.getRankedPosts(any(), any(), anyDouble(), any(), anyDouble(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(mockProjection));

        Post dbPost = new Post();
        dbPost.setPostId("post_123");
        dbPost.setCaption("Advanced Software Engineering Rulez");
        when(postRepository.findAllById(List.of("post_123"))).thenReturn(List.of(dbPost));

        List<RecommendedPost> recommendations = recommendationService.getRecommendedPosts(testUser, 0, 5, true);

        assertEquals(1, recommendations.size());
        RecommendedPost result = recommendations.get(0);

        assertEquals("post_123", result.getPost().getPostId());
        assertEquals("Advanced Software Engineering Rulez", result.getPost().getCaption());

        assertEquals(0.85, result.getExplanation().scoreInteraction());
        assertEquals(7.85, result.getExplanation().totalScore());

        verify(postRepository).getRankedPosts(
                any(), any(),
                eq(0.80),
                any(),
                eq(0.50),
                any(), any(),
                eq(0.90),
                eq(0.40),
                eq(0.60),
                eq(0.30),
                anyInt(), anyInt()
        );
    }
}