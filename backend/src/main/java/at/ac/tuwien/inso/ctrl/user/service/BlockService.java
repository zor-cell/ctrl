package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.dto.BlockedResponse;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelation;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.BlockedRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BlockService {

  private final FollowService followService;
  private final UserRepository userRepository;
  private final FriendshipService friendshipService;
  private final PostRepository postRepository;
  private final BlockedRelationRepository blockedRelationRepository;
  private final RelationshipMapper relationshipMapper;
  private final CommentRepository commentRepository;
  private final PostVoteRepository postVoteRepository;
  private final SourceVoteRepository sourceVoteRepository;

  public void blockUser(User user, String username) {
    User blockedUser =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

    if (blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(user, blockedUser)) {
      throw new ResourceAlreadyExistsException(
          "User is already blocked", user.getUsername(), username);
    }

    if (followService.isFollowing(user, blockedUser)) {
      followService.unfollow(user, username);
    }

    if (followService.isFollowing(blockedUser, user)) {
      followService.unfollow(blockedUser, user.getUsername());
    }

    if (followService.areFriends(user, blockedUser)) {
      friendshipService.unfriend(user, username);
    }

    BlockedRelation blockedRelation =
        BlockedRelation.builder()
            .since(LocalDateTime.now())
            .id(new BlockedRelationId(user.getId(), blockedUser.getId()))
            .blockedUser(blockedUser)
            .user(user)
            .build();

    blockedRelationRepository.save(blockedRelation);

    removeBlockedUserContent(user, blockedUser);
  }

  private void removeBlockedUserContent(User user, User blockedUser) {
    List<Post> posts = postRepository.findPostsByUser(user);

    posts.forEach(
        post -> {
          List<Comment> comments = commentRepository.findCommentsByUserAndPost(blockedUser, post);

          comments.forEach(
              c -> {
                Comment comment =
                    commentRepository
                        .findById(c.getCommentId())
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "Comment not found"));

                commentRepository.delete(comment);
                // commentService.deleteComment(comment.getCommentId(), blockedUser.getUsername());
              });

          postVoteRepository
              .findById(new PostVoteId(post.getPostId(), blockedUser.getId().toString()))
              .ifPresent(postVoteRepository::delete);
          if (post.getSource() != null) {
            sourceVoteRepository
                .findById(
                    new SourceVoteId(
                        post.getSource().getSourceId(), blockedUser.getId().toString()))
                .ifPresent(sourceVoteRepository::delete);
          }
        });
  }

  public boolean inBlockedRelationship(User user, User user2) {

    if (blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(user, user2)) {
      return true;
    }

    if (blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(user2, user)) {
      return true;
    }
    return false;
  }

  public void unblockUser(User currentUser, String username) {
    User blockedUser =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

    BlockedRelationId relationId = new BlockedRelationId(currentUser.getId(), blockedUser.getId());
    if (!blockedRelationRepository.existsById(relationId)) {
      throw new EntityNotFoundException("Blocked relation not found");
    }

    blockedRelationRepository.deleteById(relationId);
  }

  public List<BlockedResponse> getBlockedUsers(User currentUser, String search) {
    List<User> blockedUsers =
        search == null || search.isBlank()
            ? blockedRelationRepository.findBlockedUserByUser(currentUser)
            : blockedRelationRepository.findBlockedUserByUserAndSearch(currentUser, search);

    return blockedUsers.stream().map(relationshipMapper::toBlockedResponse).toList();
  }
}
