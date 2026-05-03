package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Vector;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final PostRepository postRepository;
    private final AlgorithmService algorithmService;

    public List<Post> getRecommendedPosts(User user, int page, int size) {
        AlgorithmConfigResponse config = algorithmService.getConfig(user);

        float[] userPreferenceVector = postRepository.calculateUserPreferenceVector(user.getId().toString());
        if (userPreferenceVector == null || userPreferenceVector.length == 0) {
            userPreferenceVector = new float[768];
        }

        int offset = page * size;

        return postRepository.findPersonalizedFeed(
                user.getId().toString(),
                userPreferenceVector,
                config.weightTopic() / 100.0,
                config.weightRecency() / 100.0,
                config.weightTrending() / 100.0,
                config.weightSourceQuality() / 100.0,
                config.weightExploration() / 100.0,
                size,
                offset
        );
    }
}
