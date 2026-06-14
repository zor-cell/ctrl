import {Component, inject, signal} from '@angular/core';
import {Router} from '@angular/router';
import {Button} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {AuthService} from '../../../auth/auth.service';
import {UserService} from '../../user.service';
import {ChatSessionService} from '../../../chat/services/chat-session.service';
import {ViewTrackingService} from '../../../post/services/view-tracking.service';
import {finalize} from 'rxjs';

@Component({
  selector: 'app-settings-logout',
  imports: [Button, Dialog],
  templateUrl: './settings-logout.component.html',
  styleUrl: './settings-logout.component.scss',
})
export class SettingsLogoutComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly chatSessionService = inject(ChatSessionService);
  private readonly viewTrackingService = inject(ViewTrackingService);
  protected readonly deleteDialogVisible = signal(false);
  protected readonly deletingAccount = signal(false);

  protected logout(): void {
    this.endSession();
  }

  protected requestAccountDeletion(): void {
    this.deleteDialogVisible.set(true);
  }

  protected cancelAccountDeletion(): void {
    if (!this.deletingAccount()) {
      this.deleteDialogVisible.set(false);
    }
  }

  protected onDeleteDialogVisibleChange(visible: boolean): void {
    if (!visible) {
      this.cancelAccountDeletion();
      return;
    }

    this.deleteDialogVisible.set(true);
  }

  protected confirmAccountDeletion(): void {
    if (this.deletingAccount()) {
      return;
    }

    this.deletingAccount.set(true);
    this.userService.deleteAccount()
      .pipe(finalize(() => this.deletingAccount.set(false)))
      .subscribe({
        next: () => this.endSession(),
      });
  }

  private endSession(): void {
    this.viewTrackingService.stopAndClear();
    this.chatSessionService.teardown();
    this.authService.logout();
    this.userService.clearUser();
    this.router.navigate(['/login']);
  }
}
