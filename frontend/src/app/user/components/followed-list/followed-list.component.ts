import { Component, inject, ViewChild, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Avatar } from 'primeng/avatar';
import { Button } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { Router } from '@angular/router';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-followed-list',
  standalone: true,
  imports: [CommonModule, Avatar, Button, MenuModule],
  templateUrl: './followed-list.component.html',
  styleUrl: './followed-list.component.scss',
})
export class FollowedListComponent {
  private relationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);

  @ViewChild('menu') menu!: Menu;

  // Input to determine if search filter is active
  searchQuery = input<string>('');

  followed = this.relationshipService.followed;
  selectedUsername: string | null = null;

  menuItems = [
    {
      label: 'Unfollow',
      icon: 'pi pi-user-minus',
      command: () => this.unfollow(),
    },
  ];

  openMenu(event: Event, username: string) {
    this.selectedUsername = username;
    this.menu.toggle(event);
  }

  unfollow() {
    if (this.selectedUsername) {
      this.relationshipService.unfollow(this.selectedUsername).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Unfollowed ${this.selectedUsername}`);
          this.eventBus.unfollowed();
          this.relationshipService.loadFollowed().subscribe();
        },
        error: () => {
          this.notificationService.showError('Failed to unfollow user');
        },
      });
    }
  }

  navigateToProfile(username: string) {
    this.router.navigate(['/profile', username]);
  }
}
