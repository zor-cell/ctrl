package at.ac.tuwien.inso.ctrl.session.dto;

public record SessionStateDto(long elapsedMinutes, long seenPosts, boolean limitReached) {}
