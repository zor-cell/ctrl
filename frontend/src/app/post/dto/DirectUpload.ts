export interface DirectUploadInitRequest {
  fileName: string;
  contentType: string;
  size: number;
}

export interface DirectUploadInitResponse {
  strategy: string;
  mediaRef: string;
  mediaUrl: string;
  contentType: string | null;
  size: number;
  uploadUrl: string;
  uploadId: string | null;
  partSize: number;
  parts: unknown[] | null;
}
