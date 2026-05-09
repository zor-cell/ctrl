import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';

import { environment } from '../../environments/environment';
import { FileUploadResponse } from './dto/FileUploadResponse';

@Injectable({
  providedIn: 'root',
})
export class FileService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/files`;

  uploadPostMedia(file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.httpClient.post<FileUploadResponse>(`${this.baseUri}/post-media`, formData);
  }

  deletePostMedia(mediaRef: string) {
    const params = new HttpParams().set('ref', mediaRef);
    return this.httpClient.delete<void>(`${this.baseUri}/post-media`, { params });
  }

  loadMediaUrl(mediaUrl: string) {
    return this.httpClient
      .get(mediaUrl, { responseType: 'blob' })
      .pipe(map(blob => URL.createObjectURL(blob)));
  }
}
