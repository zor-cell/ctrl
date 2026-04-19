import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { environment } from '../../environments/environment';
import { CreatePostRequest } from './dto/CreatePostRequest';
import { PostResponse } from './dto/PostResponse';
import { TopicResponse } from './dto/TopicResponse';

@Injectable({
  providedIn: 'root',
})
export class PostService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/posts`;
  private readonly topicUri = `${environment.apiUrl}/topics`;

  createPost(request: CreatePostRequest) {
    return this.httpClient.post<PostResponse>(this.baseUri, request);
  }

  getTopics() {
    return this.httpClient.get<TopicResponse[]>(this.topicUri);
  }
}
