package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendFriendRequest(@NotBlank @Size(max = 64) String toUsername) {}
