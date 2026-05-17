package at.ac.tuwien.inso.ctrl.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class GarageConfig {
  @Value("${garage.url}")
  private String garageUrl;

  @Value("${garage.presign-url:${garage.url}}")
  private String garagePresignUrl;

  @Value("${garage.internal-presign-url:${garage.url}}")
  private String garageInternalPresignUrl;

  @Value("${garage.access-key}")
  private String accessKey;

  @Value("${garage.secret-key}")
  private String secretKey;

  @Lazy
  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(garageUrl))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of("garage"))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  @Lazy
  @Bean
  @Primary
  public S3Presigner s3Presigner() {
    return buildS3Presigner(garagePresignUrl);
  }

  @Lazy
  @Bean
  public S3Presigner internalS3Presigner() {
    return buildS3Presigner(garageInternalPresignUrl);
  }

  private S3Presigner buildS3Presigner(String endpointUrl) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(endpointUrl))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of("garage"))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
