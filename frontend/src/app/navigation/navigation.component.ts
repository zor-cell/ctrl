import {Component, computed, inject} from '@angular/core';
import {RouterModule, Router } from '@angular/router';
import {AuthService} from "../auth/auth.service";
import { ThemeService } from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import {MenuItem} from "primeng/api";
import {Menubar} from "primeng/menubar";
import {Avatar} from "primeng/avatar";
import {UserService} from "../user/user.service";
import {TieredMenu} from "primeng/tieredmenu";
import {ChatSessionService} from '../chat/chat-session.service';
import {ChatUnreadService} from '../chat/chat-unread.service';
import {ViewTrackingService} from "../post/services/view-tracking.service";

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [ButtonModule, RouterModule, Menubar, Avatar, TieredMenu],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  private router = inject(Router);
  protected authService = inject(AuthService);
  private userService = inject(UserService);
  protected themeService = inject(ThemeService);
  private viewTrackingService = inject(ViewTrackingService);
  private readonly chatUnreadService = inject(ChatUnreadService);
  private readonly chatSessionService = inject(ChatSessionService);

  protected navItems = computed<MenuItem[]>(() => {
    const loggedIn = this.authService.isAuthenticated();
    const currentUser = this.userService.user();
    const unreadTotal =
      loggedIn && currentUser != null ? this.chatUnreadService.totalUnread() : 0;

    const items: MenuItem[] = [

    ];

    if (loggedIn) {
      items.push(
        { icon: 'pi pi-home', routerLink: '/feed' },
        { icon: 'pi pi-search', routerLink: '/search' },
        { icon: 'pi pi-plus', routerLink: '/post' },
        {
          icon: 'pi pi-comments',
          routerLink: '/chat',
          styleClass: 'nav-chat-item',
          badge:
            currentUser != null && unreadTotal > 0
              ? this.formatUnreadBadge(unreadTotal)
              : undefined,
          badgeSeverity: 'secondary',
        }
      );
    }

    return items;
  });

  private formatUnreadBadge(total: number): string {
    return total > 9 ? '+9' : String(total);
  }

  protected profileItems = computed<MenuItem[]>(() => {
    const items: MenuItem[] = [
      {label: 'Profile', icon: 'pi pi-user', routerLink: '/profile'},
      {label: 'Community', icon: 'pi pi-users', routerLink: '/followed'},
      {label: 'Settings', icon: 'pi pi-cog', routerLink: '/settings'},
    ];

    if (this.authService.isAdmin()) {
      items.push(
        {separator: true},
        {
          label: 'Admin Panel',
          icon: 'pi pi-shield',
          items: [
            { label: 'Reports', icon: 'pi pi-flag', routerLink: '/report'},
            { label: 'Verification', icon: 'pi pi-shield', routerLink: '/business-verification'}
          ]
        });
    }

    items.push(
      {separator: true},
      {
        label: 'Logout',
        icon: 'pi pi-sign-out',
        command: () => {
          this.viewTrackingService.stopAndClear();
          this.chatSessionService.teardown();
          this.authService.logout();
          this.userService.clearUser();
          this.router.navigate(['/login']);
        }
      }
    );

    return items;
  });
}
