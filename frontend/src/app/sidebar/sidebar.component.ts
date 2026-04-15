import {Component, computed, inject} from '@angular/core';
import {RouterModule, RouterOutlet, Router } from '@angular/router';
import {AuthService} from "../auth/auth.service";
import {NotificationService} from "../core/services/notification.service";
import { ThemeService } from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import { Tabs, TabList, Tab } from 'primeng/tabs';
import { UserService } from '../user/user.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  requiresAuth?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterOutlet, ButtonModule, Tabs, TabList, Tab, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  theme = inject(ThemeService);
  router = inject(Router);
  authService = inject(AuthService);
  userService = inject(UserService);
  notificationService = inject(NotificationService);

  activeRoute = computed(() =>
    this.items.find(i => this.router.url.startsWith(i.route))?.route ?? ''
  );

  isLoggedIn = computed(() => this.authService.token() !== null && this.userService.user() !== null);

  items: NavItem[] = [
    { label: 'Login',    icon: '🔑', route: '/login'    },
    { label: 'Register', icon: '📝', route: '/register' },
    { label: 'Feed',     icon: '🏠', route: '/feed'     },
    { label: 'Profile',  icon: '👤', route: '/profile'  },
    { label: 'Post',     icon: '✏️', route: '/post', requiresAuth: true },
    { label: 'Chat',     icon: '💬', route: '/chat'     },
    { label: 'Settings', icon: '⚙️', route: '/settings' },
    { label: 'Following',icon: '🔁', route: '/followed'},
  ];

  navigate(item: NavItem) {
    if (item.requiresAuth && !this.isLoggedIn()) {
      this.notificationService.showInfo('Please log in to create a post.');
      void this.router.navigate(['/login']);
      return;
    }

    void this.router.navigate([item.route]);
  }
}
