package at.ac.tuwien.inso.ctrl.post.dto.request;

import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePostRequest(
    @NotBlank(message = "must not be blank")
        @Size(max = 10000, message = "must not be longer than 10000 characters")
        String caption,
    String mediaRef,
    Boolean isPublic,
    Boolean isAiGenerated,
    List<@NotBlank(message = "must not be blank") String> topicIds,
    @Valid CreateSourceRequest source) {}
