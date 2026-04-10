import {Component, computed, inject} from '@angular/core';
import {RouterModule, RouterOutlet, Router } from '@angular/router';
import { ThemeService } from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import { Tabs, TabList, Tab } from 'primeng/tabs';

interface NavItem {
  label: string;
  icon: string;
  route: string;
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

  activeRoute = computed(() =>
    this.items.find(i => this.router.url.startsWith(i.route))?.route ?? ''
  );

  items: NavItem[] = [
    { label: 'Login',    icon: '🔑', route: '/login'    },
    { label: 'Register', icon: '📝', route: '/register' },
    { label: 'Feed',     icon: '🏠', route: '/feed'     },
    { label: 'Search',   icon: '🔍', route: '/search'   },
    { label: 'Profile',  icon: '👤', route: '/profile'  },
    { label: 'Post',     icon: '✏️', route: '/post'     },
    { label: 'Chat',     icon: '💬', route: '/chat'     },
    { label: 'Settings', icon: '⚙️', route: '/settings' },
    { label: 'Following',icon: '🔁', route: '/following'},
  ];
}
