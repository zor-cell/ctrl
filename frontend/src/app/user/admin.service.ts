import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {BusinessVerification} from './dto/BusinessVerification';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly baseUri: string = environment.apiUrl + '/admin';
  private httpClient = inject(HttpClient);

  public getPendingVerifications() {
    return this.httpClient.get<BusinessVerification[]>(this.baseUri + '/verifications/pending');
  }
}
