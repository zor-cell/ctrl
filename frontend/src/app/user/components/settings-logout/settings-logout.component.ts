import {Component, inject} from '@angular/core';
import {Router} from '@angular/router';
import {Button} from 'primeng/button';
import {AuthService} from '../../../auth/auth.service';
import {UserService} from '../../user.service';
import {ChatSessionService} from '../../../chat/services/chat-session.service';
import {ViewTrackingService} from '../../../post/services/view-tracking.service';

@Component({
  selector: 'app-settings-logout',
  imports: [Button],
  templateUrl: './settings-logout.component.html',
  styleUrl: './settings-logout.component.scss',
})
export class SettingsLogoutComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly chatSessionService = inject(ChatSessionService);
  private readonly viewTrackingService = inject(ViewTrackingService);

  protected logout(): void {
    this.viewTrackingService.stopAndClear();
    this.chatSessionService.teardown();
    this.authService.logout();
    this.userService.clearUser();
    this.router.navigate(['/login']);
  }
}
