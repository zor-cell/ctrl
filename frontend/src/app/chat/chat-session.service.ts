import { Injectable, inject } from '@angular/core';

import { ChatRealtimeBridgeService } from './chat-realtime-bridge.service';
import { ChatUnreadService } from './chat-unread.service';

@Injectable({ providedIn: 'root' })
export class ChatSessionService {
  private readonly chatUnreadService = inject(ChatUnreadService);
  private readonly chatRealtimeBridge = inject(ChatRealtimeBridgeService);

  bootstrap(): void {
    this.chatUnreadService.refresh().subscribe({
      error: () => {
        // Chat list can still be loaded from the chat page.
      },
    });
    this.chatRealtimeBridge.ensureConnected();
  }

  teardown(): void {
    this.chatRealtimeBridge.disconnect();
    this.chatUnreadService.clear();
  }
}
