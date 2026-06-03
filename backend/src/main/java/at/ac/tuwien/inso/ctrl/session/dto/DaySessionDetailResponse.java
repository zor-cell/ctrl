package at.ac.tuwien.inso.ctrl.session.dto;

import java.time.LocalDate;
import java.util.List;

public record DaySessionDetailResponse(LocalDate date, List<SessionIntervalDto> sessions) {}
