import {Component, computed, inject} from '@angular/core';
import {RouterModule, RouterOutlet, Router } from '@angular/router';
import {AuthService} from "../auth/auth.service";
import {NotificationService} from "../core/services/notification.service";
import { ThemeService } from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import { Tabs, TabList, Tab } from 'primeng/tabs';
import {MenuItem} from "primeng/api";
import {Menubar} from "primeng/menubar";
import {Avatar} from "primeng/avatar";
import {Menu} from "primeng/menu";

interface NavItem {
  label: string;
  icon: string;
  route: string;
  requiresAuth?: boolean;
}

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [RouterOutlet, ButtonModule, Tabs, TabList, Tab, RouterModule, Menubar, Avatar, Menu],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  protected authService = inject(AuthService);
  protected themeService = inject(ThemeService);

  protected activeRoute = computed(() =>
    this.items.find(i => this.router.url.startsWith(i.route))?.route ?? ''
  );

  protected navItems = computed<MenuItem[]>(() => {
    const loggedIn = this.authService.isAuthenticated();

    const items: MenuItem[] = [
      { label: 'Feed', icon: 'pi pi-home', routerLink: '/feed' },
      { label: 'Search', icon: 'pi pi-search', routerLink: '/search' }
    ];

    if (loggedIn) {
      items.push(
        { label: 'Post', icon: 'pi pi-pencil', routerLink: '/post' },
        { label: 'Chat', icon: 'pi pi-comments', routerLink: '/chat' },
        { label: 'Following', icon: 'pi pi-users', routerLink: '/followed' }
      );
    }
    return items;
  });

  protected profileItems = computed<MenuItem[]>(() => [
    { label: 'Profile', icon: 'pi pi-user', routerLink: '/profile' },
    { label: 'Settings', icon: 'pi pi-cog', routerLink: '/settings' },
    { separator: true },
    {
      label: 'Logout',
      icon: 'pi pi-sign-out',
      command: () => {
        this.authService.logout();
        this.router.navigate(['/login']);
      }
    }
  ]);

  protected items: NavItem[] = [
    { label: 'Feed',     icon: '🏠', route: '/feed'     },
    { label: 'Search',   icon: '🔍', route: '/search'   },
    { label: 'Profile',  icon: '👤', route: '/profile'  },
    { label: 'Post',     icon: '✏️', route: '/post', requiresAuth: true },
    { label: 'Chat',     icon: '💬', route: '/chat'     },
    { label: 'Settings', icon: '⚙️', route: '/settings' },
    { label: 'Following',icon: '🔁', route: '/followed'},
  ];

  protected navigate(item: NavItem) {
    if (item.requiresAuth && !this.authService.isAuthenticated()) {
      this.notificationService.showInfo('Please log in to create a post.');
      void this.router.navigate(['/login']);
      return;
    }

    void this.router.navigate([item.route]);
  }
}
