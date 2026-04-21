package at.ac.tuwien.inso.ctrl.file.dto;

public record FileUploadResponse(String mediaRef, String mediaUrl, String contentType, long size) {}
