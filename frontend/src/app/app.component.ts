import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toast } from 'primeng/toast';

import { AuthService } from './auth/auth.service';
import { ChatRealtimeBridgeService } from './chat/chat-realtime-bridge.service';
import { ChatUnreadService } from './chat/chat-unread.service';
import { NavigationComponent } from './navigation/navigation.component';
import { UserService } from './user/user.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast, NavigationComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly chatUnreadService = inject(ChatUnreadService);
  private readonly chatRealtimeBridge = inject(ChatRealtimeBridgeService);

  ngOnInit() {
    if (this.authService.validateSession()) {
      this.userService.loadUser().subscribe();
      this.chatUnreadService.refresh().subscribe({
        error: () => {
          // Chat list can still be loaded from the chat page.
        },
      });
      this.chatRealtimeBridge.ensureConnected();
    }
  }
}
