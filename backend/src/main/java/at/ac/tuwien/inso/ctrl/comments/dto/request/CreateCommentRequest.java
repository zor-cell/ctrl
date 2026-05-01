package at.ac.tuwien.inso.ctrl.comments.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(

    @NotBlank(message = "Content must not be blank")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    String content,

    @Nullable
    String parentCommentId
) {}
