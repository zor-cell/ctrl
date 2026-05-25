package at.ac.tuwien.inso.ctrl.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class IdAustriaConfig {
  @Value("${application.security.id-austria.issuer-uri}")
  private String issuerUri;

  @Value("${application.security.id-austria.jwk-uri}")
  private String jwkUri;

  @Bean
  @Qualifier("idAustriaJwtDecoder")
  public JwtDecoder idAustriaJwtDecoder() {
    // lazy discovery of oidc issuer uri
    String jwkSetUri = jwkUri + "/protocol/openid-connect/certs";
    NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

    // this is a workaround to ensure the correct issuer is used, otherwise when
    // running in
    // production for docker it doesnt work
    OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(issuerUri);
    OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

    jwtDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(withTimestamp, issuerValidator));
    return jwtDecoder;
  }
}
