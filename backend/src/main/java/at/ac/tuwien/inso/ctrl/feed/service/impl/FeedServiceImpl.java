package at.ac.tuwien.inso.ctrl.feed.service.impl;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;
    private final PostVoteRepository postVoteRepository;
    private final SourceVoteRepository sourceVoteRepository;
    private final UserRepository userRepository;
    private final FeedMapper feedMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeedPostResponse> getFeed(int page, int postsPerPage, String username) {

        Optional<String> userId = validateInputAndResolveUser(page, postsPerPage, username);

        // Step 1: Retrieve only IDs in paginated format
        var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").descending());
        List<String> postIds = postRepository.findAllPaginated(pageable)
                .map(Post::getPostId)
                .toList();

        // Step 2: Load associations for these IDs only
        List<Post> posts = postRepository.findAllWithAssociationsByIds(postIds);

        // Step 3: Retrieve votes for all posts at once
        Map<String, String> postVotes = userId
                .map(id -> postVoteRepository.findByIdUserIdAndIdPostIdIn(id, postIds)
                        .stream().collect(Collectors.toMap(
                                v -> v.getId().getPostId(),
                                PostVote::getVoteType)))
                .orElse(Map.of());

        List<String> sourceIds = posts.stream()
                .filter(p -> p.getSource() != null)
                .map(p -> p.getSource().getSourceId())
                .toList();

        Map<String, String> sourceVotes = userId
                .map(id -> sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(id, sourceIds)
                        .stream().collect(Collectors.toMap(
                                v -> v.getId().getSourceId(),
                                SourceVote::getVoteType)))
                .orElse(Map.of());

        // Step 4: Mapping
        return posts.stream()
                .map(post -> feedMapper.toFeedPostResponse(post, postVotes, sourceVotes))
                .toList();
    }
    private String fetchUserId(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private Optional<String> validateInputAndResolveUser(int page, int postsPerPage, String username) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (postsPerPage < 1 || postsPerPage > 100) {
            throw new IllegalArgumentException("PostsPerPage must be between 1 and 100");
        }

        return Optional.ofNullable(username).map(this::fetchUserId);
    }
}