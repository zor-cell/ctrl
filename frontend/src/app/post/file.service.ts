import { HttpClient, HttpHeaders } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { environment } from '../../environments/environment';
import { FileUploadResponse } from './dto/FileUploadResponse';

@Injectable({
  providedIn: 'root',
})
export class FileService {
  private readonly httpClient = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly baseUri = `${environment.apiUrl}/files`;

  uploadPostMedia(file: File) {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.httpClient.post<FileUploadResponse>(
      `${this.baseUri}/post-media`,
      formData,
      this.authHeaders(),
    );
  }

  deletePostMedia(mediaRef: string) {
    const params = new HttpParams().set('ref', mediaRef);
    return this.httpClient.delete<void>(
      `${this.baseUri}/post-media`,
      { ...this.authHeaders(), params },
    );
  }

  loadMediaUrl(mediaUrl: string) {
    return this.httpClient
      .get(mediaUrl, { responseType: 'blob' })
      .pipe(map(blob => URL.createObjectURL(blob)));
  }

  private authHeaders() {
    const token = this.authService.token();
    return token
      ? { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) }
      : {};
  }
}
