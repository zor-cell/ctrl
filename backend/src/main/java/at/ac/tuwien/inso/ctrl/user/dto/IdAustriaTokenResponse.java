package at.ac.tuwien.inso.ctrl.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IdAustriaTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("scope") String scope,
        @JsonProperty("id_token") String idToken
) {
}
