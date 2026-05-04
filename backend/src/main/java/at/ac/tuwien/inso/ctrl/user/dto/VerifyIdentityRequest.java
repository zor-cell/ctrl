package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyIdentityRequest(@NotBlank String code, String redirectUri) {
  public VerifyIdentityRequest(String code) {
    this(code, null);
  }
}
