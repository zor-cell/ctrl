package at.ac.tuwien.inso.ctrl.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddReactionRequest(
    @NotBlank(message = "must not be blank")
        @Size(max = 16, message = "must not be longer than 16 characters")
        String emoji) {}
