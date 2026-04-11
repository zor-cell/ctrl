package at.ac.tuwien.inso.ctrl.embedding;

import at.ac.tuwien.inso.ctrl.embedding.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    @Value("${infinity.url}")
    private String baseUrl;

    @Value("${infinity.model}")
    private String model;

    private final RestClient restClient = RestClient.create();

    public float[] embedText(String text) {
        var request = Map.of(
                "model", model,
                "input", List.of(text)
        );

        return callEmbeddings(request);
    }

    public float[] embedImage(String imageUrl) {
        var request = Map.of(
                "model", model,
                "input", List.of(imageUrl),
                "modality", "image"
        );

        return callEmbeddings(request);
    }

    /**
     * Makes an HTTP call to the embedding server according to OpenAI specifications:
     * <a href="https://developers.openai.com/api/reference/resources/embeddings/methods/create">OpenAI Specs</a>
     */
    public float[] embedTextWithImage(String text, String imageUrl) {
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

    private float[] callEmbeddings(Map<String, Object> request) {
        var response = restClient.post()
                .uri(baseUrl + "/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        return response.data().getFirst().embedding();
    }
}

