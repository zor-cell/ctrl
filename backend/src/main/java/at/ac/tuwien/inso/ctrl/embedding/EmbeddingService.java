package at.ac.tuwien.inso.ctrl.embedding;

import at.ac.tuwien.inso.ctrl.embedding.dto.EmbeddingRequest;
import at.ac.tuwien.inso.ctrl.embedding.dto.EmbeddingResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    @Value("${infinity.url}")
    private String baseUrl;

    @Value("${infinity.model}")
    private String model;

    //HTTP1 protocol has to be used!
    private final RestClient restClient = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_1_1)
                            .build()
            ))
            .build();

    public float[] embedText(String text) {
        var request = new EmbeddingRequest(model, List.of(text), null);

        return callEmbeddings(request);
    }

    public float[] embedImage(String imageUrl) {
        var request = new EmbeddingRequest(model, List.of(imageUrl), "image");

        return callEmbeddings(request);
    }

    /**
     * Makes an HTTP call to the embedding server according to OpenAI specifications:
     * <a href="https://developers.openai.com/api/reference/resources/embeddings/methods/create">OpenAI Specs</a>
     */
    public float[] embedTextWithImage(String text, String imageUrl) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasImage = imageUrl != null && !imageUrl.isBlank();

        if(hasText && !hasImage) return embedText(text);
        if(!hasText && hasImage) return embedImage(imageUrl);

        float[] textVec = embedText(text);
        float[] imageVec = embedImage(imageUrl);

        //average embeddings from text and image
        float sumSquares = 0f;
        float[] fused = new float[textVec.length];
        for (int i = 0; i < textVec.length; i++) {
            fused[i] = (textVec[i] + imageVec[i]) / 2.0f;
            sumSquares += fused[i] * fused[i];
        }

        //normalize vector
        float magnitude = (float) Math.sqrt(sumSquares);
        for (int i = 0; i < fused.length; i++) {
            fused[i] /= magnitude;
        }

        return fused;
    }

    private float[] callEmbeddings(EmbeddingRequest request) {
        var response = restClient.post()
                .uri(baseUrl + "/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        return response.data().getFirst().embedding();
    }
}

