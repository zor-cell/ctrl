import { Component, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { ThemeService } from '../core/services/theme.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  themeService = inject(ThemeService);
  collapsed = signal(false);

  devItems = [
    { label: 'Login',    icon: '🔑', route: '/login'    },
    { label: 'Register', icon: '📝', route: '/register' },
    { label: 'Feed',     icon: '🏠', route: '/feed'     },
    { label: 'Search',   icon: '🔍', route: '/search'   },
    { label: 'Profile',  icon: '👤', route: '/profile'  },
    { label: 'Post',     icon: '✏️', route: '/post'     },
    { label: 'Chat',     icon: '💬', route: '/chat'     },
    { label: 'Settings', icon: '⚙️', route: '/settings' },
  ];
}
