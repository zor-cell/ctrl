package at.ac.tuwien.inso.ctrl.embedding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmbeddingRequest(String model, List<String> input, String modality) {}
