import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { environment } from '../../environments/environment';
import { CreatePostRequest } from './dto/CreatePostRequest';
import { LabelResponse } from './dto/LabelResponse';
import { PostResponse } from './dto/PostResponse';

@Injectable({
  providedIn: 'root',
})
export class PostService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/posts`;
  private readonly labelUri = `${environment.apiUrl}/labels`;

  createPost(request: CreatePostRequest) {
    return this.httpClient.post<PostResponse>(this.baseUri, request);
  }

  getLabels() {
    return this.httpClient.get<LabelResponse[]>(this.labelUri);
  }
}
