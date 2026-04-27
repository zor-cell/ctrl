package at.ac.tuwien.inso.ctrl.feed.service.impl;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FeedMapper feedMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeedPostResponse> getFeed(int page, int postsPerPage, String username) {

        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (postsPerPage < 1 || postsPerPage > 100) {
            throw new IllegalArgumentException("PostsPerPage must be between 1 and 100");
        }

        Optional<String> userId = Optional.ofNullable(username)
                .map(this::fetchUserId);

        var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").descending());
        return postRepository.findAll(pageable)
                .stream()
                .map(post -> feedMapper.toFeedPostResponse(post, userId.orElse(null)))
                .toList();
    }

    /** Helper **/
    private String fetchUserId(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getId().toString())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}