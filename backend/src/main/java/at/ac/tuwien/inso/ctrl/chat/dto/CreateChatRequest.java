package at.ac.tuwien.inso.ctrl.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateChatRequest(
    @Size(max = 80, message = "must not be longer than 80 characters") String name,
    @NotEmpty(message = "must contain at least one username")
        List<@Size(min = 1, max = 255) String> usernames) {}
