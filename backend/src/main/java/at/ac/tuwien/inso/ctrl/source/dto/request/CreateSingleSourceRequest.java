package at.ac.tuwien.inso.ctrl.source.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateSingleSourceRequest(
    @Size(max = 255, message = "must not be longer than 255 characters") String journal,
    @Size(max = 255, message = "must not be longer than 255 characters") String author,
    @Size(max = 500, message = "must not be longer than 500 characters") String title,
    @Size(max = 2048, message = "must not be longer than 2048 characters") String url,
    LocalDate date) {}
