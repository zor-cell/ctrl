package at.ac.tuwien.inso.ctrl.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class GarageConfig {
  @Value("${garage.url}")
  private String garageUrl;

  @Value("${garage.access-key}")
  private String accessKey;

  @Value("${garage.secret-key}")
  private String secretKey;


  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(garageUrl))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.US_EAST_1)
        .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
        .build();
  }
}
