package at.ac.tuwien.inso.ctrl.file.dto;

public record DirectUploadInitResponse(
    String strategy,
    String mediaRef,
    String mediaUrl,
    String contentType,
    long size,
    String uploadUrl,
    String uploadId,
    long partSize,
    Object parts) {}
