import {Component, computed, effect, inject, signal} from '@angular/core';
import {RouterModule} from '@angular/router';
import {AuthService} from "../auth/auth.service";
import {ThemeService} from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import {MenuItem} from "primeng/api";
import {Menubar} from "primeng/menubar";
import {UserService} from "../user/user.service";
import {MediaCacheService} from '../core/services/media-cache.service';
import {ChatUnreadService} from '../chat/chat-unread.service';

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [ButtonModule, RouterModule, Menubar],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  protected authService = inject(AuthService);
  protected userService = inject(UserService);
  protected themeService = inject(ThemeService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly chatUnreadService = inject(ChatUnreadService);

  protected profilePictureUrl = signal<string | null>(null);

  constructor() {
    // Wenn sich der User ändert (Login/Logout/Profilbild-Update), URL neu auflösen
    effect(() => {
      const rawUrl = this.userService.user()?.profilePicture ?? null;
      this.resolveProfilePicture(rawUrl);
    });
  }

  private resolveProfilePicture(rawUrl: string | null): void {
    if (!rawUrl) {
      this.profilePictureUrl.set(null);
      return;
    }

    if (!this.mediaCacheService.isBackendManagedMediaUrl(rawUrl)) {
      this.profilePictureUrl.set(rawUrl);
      return;
    }

    this.mediaCacheService.resolveMediaUrl(rawUrl, 'image', 'high').subscribe({
      next: blobUrl => this.profilePictureUrl.set(blobUrl),
      error: () => this.profilePictureUrl.set(null),
    });
  }

  protected navItems = computed<MenuItem[]>(() => {
    const loggedIn = this.authService.isAuthenticated();
    const currentUser = this.userService.user();
    const unreadTotal =
      loggedIn && currentUser != null ? this.chatUnreadService.totalUnread() : 0;

    const items: MenuItem[] = [];

    if (loggedIn) {
      items.push(
        {icon: 'pi pi-home', routerLink: '/feed'},
        {icon: 'pi pi-search', routerLink: '/search'},
        {icon: 'pi pi-plus', routerLink: '/post'},
        {
          icon: 'pi pi-comments',
          routerLink: '/chat',
          styleClass: 'nav-chat-item',
          badge: currentUser != null && unreadTotal > 0
            ? this.formatUnreadBadge(unreadTotal)
            : undefined,
          badgeSeverity: 'secondary',
        },
        {
          icon: 'pi pi-user',
          routerLink: '/profile',
          styleClass: 'nav-profile-item',
        }
      );

      if (this.authService.isAdmin()) {
        items.push({
          icon: 'pi pi-shield',
          styleClass: 'nav-admin-item',
          items: [
            {label: 'Reports', icon: 'pi pi-flag', routerLink: '/report'},
            {label: 'Verification', icon: 'pi pi-shield', routerLink: '/business-verification'}
          ]
        });
      }
    }

    return items;
  });

  private formatUnreadBadge(total: number): string {
    return total > 9 ? '+9' : String(total);
  }
}
