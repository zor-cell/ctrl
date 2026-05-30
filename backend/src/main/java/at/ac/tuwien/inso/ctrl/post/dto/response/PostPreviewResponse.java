package at.ac.tuwien.inso.ctrl.post.dto.response;

public record PostPreviewResponse(
    String postId,
    String caption,
    String mediaUrl,
    String userId,
    String username,
    String userProfilePicture,
    boolean isPublic,
    boolean isExternal,
    String userProfileUrl) {}
