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

    private final RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();

    public float[] embedText(String text) {
        var request = Map.of(
                "model", model,
                "input", List.of(text)
        );

        var response = restClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        return response.data().get(0).embedding();
    }
}

