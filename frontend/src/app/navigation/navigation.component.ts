import {Component, computed, inject} from '@angular/core';
import {RouterModule, Router } from '@angular/router';
import {AuthService} from "../auth/auth.service";
import { ThemeService } from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import {MenuItem} from "primeng/api";
import {Menubar} from "primeng/menubar";
import {Avatar} from "primeng/avatar";
import {Menu} from "primeng/menu";

@Component({
  selector: 'app-navigation',
  standalone: true,
  imports: [ButtonModule, RouterModule, Menubar, Avatar, Menu],
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.scss'
})
export class NavigationComponent {
  private router = inject(Router);
  protected authService = inject(AuthService);
  protected themeService = inject(ThemeService);

  protected navItems = computed<MenuItem[]>(() => {
    const loggedIn = this.authService.isAuthenticated();

    const items: MenuItem[] = [
      { label: 'Feed', icon: 'pi pi-home', routerLink: '/feed' },
      { label: 'Search', icon: 'pi pi-search', routerLink: '/search' }
    ];

    if (loggedIn) {
      items.push(
        { label: 'Post', icon: 'pi pi-plus', routerLink: '/post' },
        { label: 'Chat', icon: 'pi pi-comments', routerLink: '/chat' }
      );
    }
    return items;
  });

  protected profileItems = computed<MenuItem[]>(() => {
    const items: MenuItem[] = [
      {label: 'Profile', icon: 'pi pi-user', routerLink: '/profile'},
      {label: 'Community', icon: 'pi pi-users', routerLink: '/followed'},
      {label: 'Settings', icon: 'pi pi-cog', routerLink: '/settings'},
      { label: 'Algorithm', icon: 'pi pi-cog', routerLink: '/algorithm' },
    ];

    if (this.authService.isAdmin()) {
      items.push(
        {separator: true},
        {
          label: 'Admin Panel',
          icon: 'pi pi-shield',
          routerLink: '/admin'
        });
    }

    items.push(
      {separator: true},
      {
        label: 'Logout',
        icon: 'pi pi-sign-out',
        command: () => {
          this.authService.logout();
          this.router.navigate(['/login']);
        }
      }
    );

    return items;
  });
}
