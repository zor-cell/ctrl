import {Component, computed, inject} from '@angular/core';
import {RouterModule, Router } from '@angular/router';
import {AuthService} from "../auth/auth.service";
import { ThemeService } from '../core/services/theme.service';
import {ButtonModule} from 'primeng/button';
import {MenuItem} from "primeng/api";
import {Menubar} from "primeng/menubar";
import {Avatar} from "primeng/avatar";
import {Menu} from "primeng/menu";
import {UserService} from "../user/user.service";

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
  private userService = inject(UserService);
  protected themeService = inject(ThemeService);

  protected navItems = computed<MenuItem[]>(() => {
    const loggedIn = this.authService.isAuthenticated();

    const items: MenuItem[] = [

    ];

    if (loggedIn) {
      items.push(
        { icon: 'pi pi-home', routerLink: '/feed' },
        { icon: 'pi pi-search', routerLink: '/search' },
        { icon: 'pi pi-plus', routerLink: '/post' },
        { icon: 'pi pi-comments', routerLink: '/chat' }
      );
    }

    if (this.authService.isAdmin()) {
      items.push(
        { label: 'Report', icon: 'pi pi-flag', routerLink: '/report'}
      );
    }
    return items;
  });

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
          this.userService.clearUser();
          this.router.navigate(['/login']);
        }
      }
    );

    return items;
  });
}
