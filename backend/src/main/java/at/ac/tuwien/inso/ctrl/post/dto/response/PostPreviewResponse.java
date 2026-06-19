package at.ac.tuwien.inso.ctrl.post.dto.response;

public record PostPreviewResponse(
    String postId,
    String caption,
    String mediaUrl,
    String userId,
    String username,
    String userProfilePicture,
    boolean userDeleted,
    boolean isPublic,
    boolean isExternal,
    String userProfileUrl) {}
