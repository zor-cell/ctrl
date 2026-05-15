import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../environments/environment';
import { Chat, ChatMessage, CreateChatRequest, SendMessageRequest } from './chat.models';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/chats`;

  getChats() {
    return this.httpClient.get<Chat[]>(this.baseUri);
  }

  createChat(request: CreateChatRequest) {
    return this.httpClient.post<Chat>(this.baseUri, request);
  }

  getMessages(chatId: string) {
    return this.httpClient.get<ChatMessage[]>(`${this.baseUri}/${chatId}/messages`);
  }

  sendMessage(chatId: string, request: SendMessageRequest) {
    return this.httpClient.post<ChatMessage>(`${this.baseUri}/${chatId}/messages`, request);
  }

  deleteChat(chatId: string) {
    return this.httpClient.delete<void>(`${this.baseUri}/${chatId}`);
  }
}
