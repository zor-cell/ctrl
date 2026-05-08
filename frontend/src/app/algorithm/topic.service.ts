import {inject, Injectable} from '@angular/core';
import {TopicResponse} from "../post/dto/TopicResponse";
import {HttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";

@Injectable({
  providedIn: 'root',
})
export class TopicService {
  private readonly baseUri: string = environment.apiUrl + '/topics';
  private readonly httpClient = inject(HttpClient);

  public getTopics() {
    return this.httpClient.get<TopicResponse[]>(this.baseUri);
  }
}
