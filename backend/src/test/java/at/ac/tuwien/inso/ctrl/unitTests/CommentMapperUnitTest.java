package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;

import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.comments.mapper.CommentMapper;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentMapperUnitTest {

    @InjectMocks private CommentMapper commentMapper;

    /** toResponse – core field mapping */
    @Test
    void toResponse_withActiveUser_shouldMapAllFieldsCorrectly() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Hello world", false, null,
                LocalDateTime.of(2026, 4, 25, 12, 0), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertEquals("c-1", response.commentId());
        assertEquals(user.getId(), response.userId());
        assertEquals("alice", response.username());
        assertEquals("alice.jpg", response.userProfilePic());
        assertEquals(LocalDateTime.of(2026, 4, 25, 12, 0).toString(), response.createdAt());
        assertEquals("Hello world", response.content());
        assertNull(response.parentCommentId());
        assertFalse(response.edited());
        assertFalse(response.userDeleted());
        assertTrue(response.replies().isEmpty());
        assertFalse(response.wasReportedByUser());
    }

    /** toResponse – deleted user */
    @Test
    void toResponse_withDeletedUser_shouldUseDeletedUserLabelAndNullProfilePicture() {
        User user = buildUser("alice", "alice.jpg", true);
        Comment comment = buildComment("c-1", user, "Some content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertEquals("Deleted user", response.username());
        assertNull(response.userProfilePic());
        assertTrue(response.userDeleted());
    }

    @Test
    void toResponse_withDeletedUser_shouldStillMapOtherFieldsCorrectly() {
        User user = buildUser("bob", "bob.jpg", true);
        Comment comment = buildComment("c-2", user, "Some content", false, null,
                LocalDateTime.of(2026, 1, 1, 10, 0), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertEquals("c-2", response.commentId());
        assertEquals(user.getId(), response.userId());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0).toString(), response.createdAt());
    }

    /** toResponse – removed comment */
    @Test
    void toResponse_withRemovedComment_shouldReplaceContentWithDeletedPlaceholder() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Original content", true, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertEquals("This comment was deleted.", response.content());
    }

    @Test
    void toResponse_withNotRemovedComment_shouldReturnOriginalContent() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Original content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertEquals("Original content", response.content());
    }

    /** toResponse – parent comment */
    @Test
    void toResponse_withParentComment_shouldIncludeParentCommentId() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment parent = buildComment("parent-1", user, "Parent", false, null,
                LocalDateTime.now(), false);
        Comment reply = buildComment("reply-1", user, "Reply", false, parent,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(reply, Map.of(), Map.of());

        assertEquals("parent-1", response.parentCommentId());
    }

    @Test
    void toResponse_withoutParentComment_shouldHaveNullParentCommentId() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Top-level comment", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertNull(response.parentCommentId());
    }

    /** toResponse – edited flag */
    @Test
    void toResponse_withEditedComment_shouldReturnEditedTrue() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Edited content", false, null,
                LocalDateTime.now(), true);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertTrue(response.edited());
    }

    @Test
    void toResponse_withUnEditedComment_shouldReturnEditedFalse() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Original content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertFalse(response.edited());
    }

    /** toResponse – reported by user */
    @Test
    void toResponse_withCommentReportedByUser_shouldReturnReportedByUserTrue() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Some content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(
                comment, Map.of(), Map.of("c-1", Boolean.TRUE));

        assertTrue(response.wasReportedByUser());
    }

    @Test
    void toResponse_withCommentNotReportedByUser_shouldReturnReportedByUserFalse() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Some content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(
                comment, Map.of(), Map.of("c-1", Boolean.FALSE));

        assertFalse(response.wasReportedByUser());
    }

    @Test
    void toResponse_withNullReportedByUserMap_shouldReturnReportedByUserFalse() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Some content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), null);

        assertFalse(response.wasReportedByUser());
    }

    @Test
    void toResponse_withCommentIdMissingFromReportedMap_shouldReturnReportedByUserFalse() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Some content", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(
                comment, Map.of(), Map.of("other-comment", Boolean.TRUE));

        assertFalse(response.wasReportedByUser());
    }

    /** toResponse – nested replies */
    @Test
    void toResponse_withNestedReply_shouldIncludeReplyInResponse() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment parent = buildComment("parent-1", user, "Parent comment", false, null,
                LocalDateTime.of(2026, 4, 25, 10, 0), false);
        Comment reply = buildComment("reply-1", user, "Reply comment", false, parent,
                LocalDateTime.of(2026, 4, 25, 11, 0), false);

        Map<String, List<Comment>> repliesByParent = Map.of("parent-1", List.of(reply));

        CommentResponse response = commentMapper.toResponse(parent, repliesByParent, Map.of());

        assertEquals(1, response.replies().size());
        assertEquals("reply-1", response.replies().get(0).commentId());
        assertEquals("Reply comment", response.replies().get(0).content());
    }

    @Test
    void toResponse_withNoReplies_shouldReturnEmptyRepliesList() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment comment = buildComment("c-1", user, "Top-level comment", false, null,
                LocalDateTime.now(), false);

        CommentResponse response = commentMapper.toResponse(comment, Map.of(), Map.of());

        assertTrue(response.replies().isEmpty());
    }

    @Test
    void toResponse_withDeeplyNestedReplies_shouldRecursivelyMapAllLevels() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment grandparent = buildComment("gp-1", user, "Grandparent", false, null,
                LocalDateTime.now(), false);
        Comment parent = buildComment("p-1", user, "Parent", false, grandparent,
                LocalDateTime.now(), false);
        Comment child = buildComment("c-1", user, "Child", false, parent,
                LocalDateTime.now(), false);

        Map<String, List<Comment>> repliesByParent = Map.of(
                "gp-1", List.of(parent),
                "p-1", List.of(child));

        CommentResponse response = commentMapper.toResponse(grandparent, repliesByParent, Map.of());

        assertEquals(1, response.replies().size());
        CommentResponse parentResponse = response.replies().get(0);
        assertEquals("p-1", parentResponse.commentId());
        assertEquals(1, parentResponse.replies().size());
        assertEquals("c-1", parentResponse.replies().get(0).commentId());
    }

    @Test
    void toResponse_withMultipleReplies_shouldMapAllReplies() {
        User user = buildUser("alice", "alice.jpg", false);
        Comment parent = buildComment("parent-1", user, "Parent", false, null,
                LocalDateTime.now(), false);
        Comment reply1 = buildComment("reply-1", user, "First reply", false, parent,
                LocalDateTime.now(), false);
        Comment reply2 = buildComment("reply-2", user, "Second reply", false, parent,
                LocalDateTime.now(), false);

        Map<String, List<Comment>> repliesByParent = Map.of("parent-1", List.of(reply1, reply2));

        CommentResponse response = commentMapper.toResponse(parent, repliesByParent, Map.of());

        assertEquals(2, response.replies().size());
    }

    /** Helpers */
    private User buildUser(String username, String profilePicture, boolean deleted) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setProfilePicture(profilePicture);
        user.setDeleted(deleted);
        return user;
    }

    private Comment buildComment(
            String commentId,
            User user,
            String content,
            boolean isRemoved,
            Comment parentComment,
            LocalDateTime createdAt,
            boolean edited) {
        Comment comment = new Comment();
        comment.setCommentId(commentId);
        comment.setUser(user);
        comment.setContent(content);
        comment.setIsRemoved(isRemoved);
        comment.setParentComment(parentComment);
        comment.setCreatedAt(createdAt);
        comment.setEdited(edited);
        return comment;
    }
}