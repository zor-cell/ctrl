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

    public float[] embedTextWithImage(String text, String imageUrl) {
        //average the embeddings from text and image
        float[] textVec = embedText(text);
        float[] imageVec = embedImage(imageUrl);

        float[] fused = new float[textVec.length];
        for (int i = 0; i < textVec.length; i++) {
            fused[i] = (textVec[i] + imageVec[i]) / 2.0f;
        }
        return fused;
    }

    private float[] callEmbeddings(Map<String, Object> request) {
        var response = restClient.post()
                .uri(baseUrl + "/v1/embeddings")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        return response.data().getFirst().embedding();
    }
}

