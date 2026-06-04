import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../environments/environment';
import { SILENT_ERROR_CONTEXT } from '../core/interceptors';
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

  getChat(chatId: string) {
    return this.httpClient.get<Chat>(`${this.baseUri}/${chatId}`);
  }

  createChat(request: CreateChatRequest) {
    return this.httpClient.post<Chat>(this.baseUri, request);
  }

  getMessages(chatId: string) {
    return this.httpClient.get<ChatMessage[]>(`${this.baseUri}/${chatId}/messages`);
  }

  markChatRead(chatId: string) {
    return this.httpClient.post<void>(
      `${this.baseUri}/${chatId}/read`,
      {},
      { context: SILENT_ERROR_CONTEXT }
    );
  }

  sendMessage(chatId: string, request: SendMessageRequest) {
    return this.httpClient.post<ChatMessage>(`${this.baseUri}/${chatId}/messages`, request);
  }

  deleteChat(chatId: string) {
    return this.httpClient.delete<void>(`${this.baseUri}/${chatId}`);
  }

  leaveChat(chatId: string) {
    return this.httpClient.delete<void>(`${this.baseUri}/${chatId}/members/me`);
  }

  addMembers(chatId: string, usernames: string[]) {
    return this.httpClient.post<Chat>(`${this.baseUri}/${chatId}/members`, { usernames });
  }

  removeMember(chatId: string, memberId: string) {
    return this.httpClient.delete<Chat>(`${this.baseUri}/${chatId}/members/${memberId}`);
  }

  reactToMessage(chatId: string, messageId: string, emoji: string) {
    return this.httpClient.post<ChatMessage>(
      `${this.baseUri}/${chatId}/messages/${messageId}/reactions`,
      { emoji }
    );
  }

  uploadGroupImage(chatId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return this.httpClient.post<Chat>(`${this.baseUri}/${chatId}/group-image`, formData);
  }
}
