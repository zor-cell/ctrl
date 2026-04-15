package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDate;

public record SingleSourceResponse(
    String singleSourceId,
    String journal,
    String author,
    String title,
    String url,
    LocalDate date) {}
