package at.ac.tuwien.inso.ctrl.source.dto.request;

import java.time.LocalDate;

public record CreateSingleSourceRequest(
    String journal, String author, String title, String url, LocalDate date) {}
