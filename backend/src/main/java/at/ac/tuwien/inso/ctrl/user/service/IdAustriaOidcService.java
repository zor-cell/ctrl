package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.user.dto.IdAustriaTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class IdAustriaOidcService {
    @Value("${application.security.id-austria.client-id}")
    private String clientId;

    @Value("${application.security.id-austria.client-secret}")
    private String clientSecret;

    @Value("${application.security.id-austria.token-uri}")
    private String tokenUri;

    @Value("${application.security.id-austria.redirect-uri}")
    private String redirectUri;

    private final RestClient restClient = RestClient.create();

    private final JwtDecoder idAustriaJwtDecoder;

    private IdAustriaTokenResponse exchangeCodeForToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("client_id", clientId);

        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(IdAustriaTokenResponse.class);
    }

    /**
     * Makes an HTTP call to the IDAustria OIDC endpoint specified in their specification:
     * <a href="https://www.id-austria.gv.at/de/developer/anbinden/anbindung-mit-openid-connect">ID Austria OIDC Spezifikation</a>
     */
    public Jwt getTokenFromCode(String code) {
        IdAustriaTokenResponse tokenResponse = exchangeCodeForToken(code);

        return idAustriaJwtDecoder.decode(tokenResponse.idToken());
    }
}
