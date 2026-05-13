package at.ac.tuwien.inso.ctrl.embedding;

import at.ac.tuwien.inso.ctrl.embedding.dto.EmbeddingData;
import at.ac.tuwien.inso.ctrl.embedding.dto.EmbeddingRequest;
import at.ac.tuwien.inso.ctrl.embedding.dto.EmbeddingResponse;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import java.net.http.HttpClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
  @Value("${infinity.url}")
  private String baseUrl;

  @Value("${infinity.model}")
  private String model;

  //weights to be applied when creating shared embeddings (sum must equal 1)
  private float textWeight = 0.6f;
  private float imageWeight = 0.4f;

  // HTTP1 protocol has to be used!
  private final RestClient restClient =
      RestClient.builder()
          .requestFactory(
              new JdkClientHttpRequestFactory(
                  HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
          .build();

  /**
   * Embeds the given string into a vector
   */
  public float[] embedText(String text) {
    var request = new EmbeddingRequest(model, List.of(text), null);

    List<EmbeddingData> embeddings = callEmbeddings(request);
    return embeddings.getFirst().embedding();
  }

  /**
   * Embeds the given strings as a batch call into a vectors (order remains the same)
   */
  public List<float[]> embedText(List<String> textList) {
    var request = new EmbeddingRequest(model, textList, null);

    List<EmbeddingData> embeddings = callEmbeddings(request);
    return embeddings.stream()
            .map(EmbeddingData::embedding)
            .toList();
  }

  /**
   * Embeds the given image url into a vector
   */
  public float[] embedImage(String imageUrl) {
    var request = new EmbeddingRequest(model, List.of(imageUrl), "image");

    List<EmbeddingData> embeddings = callEmbeddings(request);
    return embeddings.getFirst().embedding();
  }

  /**
   * Makes an HTTP call to the embedding server according to OpenAI specifications: <a
   * href="https://developers.openai.com/api/reference/resources/embeddings/methods/create">OpenAI
   * Specs</a>
   */
  public float[] embedTextWithImage(String text, String imageUrl) {
    boolean hasText = text != null && !text.isBlank();
    boolean hasImage = imageUrl != null && !imageUrl.isBlank();

    if (hasText && !hasImage) return embedText(text);
    if (!hasText && hasImage) return embedImage(imageUrl);

    float[] textVec = embedText(text);
    float[] imageVec = embedImage(imageUrl);

    // average embeddings from text and image
    float sumSquares = 0f;
    float[] fused = new float[textVec.length];
    for (int i = 0; i < textVec.length; i++) {
      fused[i] = (textVec[i] * textWeight) + (imageVec[i] * imageWeight);
      sumSquares += fused[i] * fused[i];
    }

    // normalize vector
    float magnitude = (float) Math.sqrt(sumSquares);
    if(magnitude > 0) {
      for (int i = 0; i < fused.length; i++) {
        fused[i] /= magnitude;
      }
    }

    return fused;
  }

  private List<EmbeddingData> callEmbeddings(EmbeddingRequest request) {
    try {
      var response =
          restClient
              .post()
              .uri(baseUrl + "/embeddings")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(EmbeddingResponse.class);

      return response.data();
    } catch (ResourceAccessException e) {
      throw new CommunicationException(
          "The embedding server is currently unreachable. Please try again later.");
    }
  }
}
