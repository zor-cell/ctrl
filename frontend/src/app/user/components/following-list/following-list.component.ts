import { Component, inject, ViewChild, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Avatar } from 'primeng/avatar';
import { Button } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { Router } from '@angular/router';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../services/friend-request-event-bus.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-following-list',
  standalone: true,
  imports: [CommonModule, Avatar, Button, MenuModule],
  templateUrl: './following-list.component.html',
  styleUrl: './following-list.component.scss',
})
export class FollowingListComponent {
  private socialService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);

  @ViewChild('menu') menu!: Menu;

  // Input to determine if search filter is active
  searchQuery = input<string>('');

  following = this.socialService.following;
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
      this.socialService.unfollow(this.selectedUsername).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Unfollowed ${this.selectedUsername}`);
          this.eventBus.unfollowed();
          this.socialService.loadFollowing().subscribe();
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
