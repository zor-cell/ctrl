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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final PostRepository postRepository;
    private final AlgorithmService algorithmService;

    public List<Post> getRecommendedPosts(User user, int page, int size) {
        AlgorithmConfigResponse config = algorithmService.getConfig(user);

        List<Float> vectorList = postRepository.calculateUserPreferenceVector(user.getId().toString());
        float[] userVector;
        if (vectorList == null || vectorList.isEmpty()) {
            userVector = new float[768];
        } else {
            userVector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                userVector[i] = vectorList.get(i) != null ? vectorList.get(i) : 0f;
            }
        }

        int offset = page * size;

        return postRepository.findPersonalizedFeed(
                user.getId().toString(),
                userVector,
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
