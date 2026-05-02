import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient} from "@angular/common/http";

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly baseUri: string = environment.apiUrl + '/admin';
  private httpClient = inject(HttpClient);

  public getPendingVerifications() {
    return this.httpClient.get<void>(this.baseUri + '/verifications/pending');
  }
}
