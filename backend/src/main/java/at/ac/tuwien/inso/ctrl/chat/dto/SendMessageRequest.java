package at.ac.tuwien.inso.ctrl.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotBlank(message = "must not be blank")
        @Size(max = 4000, message = "must not be longer than 4000 characters")
        String content) {}
