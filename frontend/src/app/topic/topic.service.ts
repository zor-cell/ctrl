import {inject, Injectable} from '@angular/core';
import {TopicResponse} from "./dto/TopicResponse";
import {HttpClient} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {TopicCreateRequest} from "./dto/TopicCreateRequest";
import {tap} from "rxjs";
import {NotificationService} from "../core/services/notification.service";

@Injectable({
  providedIn: 'root',
})
export class TopicService {
  private readonly baseUri: string = environment.apiUrl + '/topics';
  private readonly httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public getTopics() {
    return this.httpClient.get<TopicResponse[]>(this.baseUri);
  }

  public createTopic(topic: TopicCreateRequest) {
    return this.httpClient.post<TopicResponse>(this.baseUri, topic).pipe(
      tap(() => {
        this.notificationService.showSuccess('Topic created');
      }));
  }
}
