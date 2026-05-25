import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Toast } from 'primeng/toast';

import { AuthService } from './auth/auth.service';
import { isPublicAuthRoute } from './auth/auth-routes';
import { ChatSessionService } from './chat/chat-session.service';
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
  private readonly chatSessionService = inject(ChatSessionService);
  private readonly router = inject(Router);

  ngOnInit() {
    if (!this.authService.validateSession() || isPublicAuthRoute(this.router.url)) {
      return;
    }

    this.userService.loadUser().subscribe({
      next: () => this.chatSessionService.bootstrap(),
    });
  }
}
