package at.ac.tuwien.inso.ctrl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@Configuration
public class KeyCloakConfig {
    @Value("${application.security.id-austria.issuer-uri}")
    private String issuerUri;

    @Bean
    public JwtDecoder keycloakJwtDecoder() {
        // This automatically fetches the public keys from Keycloak to verify token signatures
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
