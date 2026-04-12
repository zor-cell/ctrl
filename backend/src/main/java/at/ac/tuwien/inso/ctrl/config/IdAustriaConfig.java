package at.ac.tuwien.inso.ctrl.config;

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

    @Bean
    public JwtDecoder idAustriaJwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        //this is a workaround to ensure the correct issuer is used, otherwise when running in production for docker it doesnt work
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator("http://localhost:8081/realms/id-austria-mock");
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withTimestamp, issuerValidator));
        return jwtDecoder;
    }
}
