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
  selector: 'app-friend-list',
  standalone: true,
  imports: [CommonModule, Avatar, Button, MenuModule],
  templateUrl: './friend-list.component.html',
  styleUrl: './friend-list.component.scss',
})
export class FriendListComponent {
  private socialService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  @ViewChild('menu') menu!: Menu;

  // Input to determine if search filter is active
  searchQuery = input<string>('');

  friends = this.socialService.friends;
  selectedUsername: string | null = null;

  menuItems = [
    {
      label: 'Remove Friend',
      icon: 'pi pi-user-minus',
      command: () => this.removeFriend(),
    },
  ];

  openMenu(event: Event, username: string) {
    this.selectedUsername = username;
    this.menu.toggle(event);
  }

  removeFriend() {
    if (this.selectedUsername) {
      this.socialService.unfollow(this.selectedUsername).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Removed ${this.selectedUsername} from friends`);
          this.eventBus.unfollowed();
          this.socialService.loadFriends().subscribe();
        },
        error: () => {
          this.notificationService.showError('Failed to remove friend');
        },
      });
    }
  }

  navigateToProfile(username: string) {
    this.router.navigate(['/profile', username]);
  }
}
