import { Component, effect, inject, input, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Button } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { Router } from '@angular/router';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { Friend } from '../../dto/Relationship';

@Component({
  selector: 'app-friend-list',
  standalone: true,
  imports: [CommonModule, Button, MenuModule],
  templateUrl: './friend-list.component.html',
  styleUrl: './friend-list.component.scss',
})
export class FriendListComponent {
  private relationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private notificationService = inject(NotificationService);
  private mediaCacheService = inject(MediaCacheService);
  private router = inject(Router);

  @ViewChild('menu') menu!: Menu;

  // Input to determine if search filter is active
  searchQuery = input<string>('');

  friends = this.relationshipService.friends;
  profilePictureUrls = signal<Record<string, string>>({});
  failedProfilePictures = signal<Set<string>>(new Set());
  selectedUsername: string | null = null;

  menuItems = [
    {
      label: 'Remove Friend',
      icon: 'pi pi-user-minus',
      command: () => this.removeFriend(),
    },
  ];

  constructor() {
    effect(() => {
      this.loadProfilePictures(this.friends());
    });
  }

  openMenu(event: Event, username: string) {
    this.selectedUsername = username;
    this.menu.toggle(event);
  }

  removeFriend() {
    if (this.selectedUsername) {
      this.relationshipService.unfriend(this.selectedUsername).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Removed ${this.selectedUsername} from friends`);
          this.eventBus.unfollowed();
          this.relationshipService.loadFriends().subscribe();
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

  hasProfilePicture(friend: Friend): boolean {
    return Boolean(this.profilePictureUrl(friend)) && !this.failedProfilePictures().has(friend.id);
  }

  profilePictureUrl(friend: Friend): string | null {
    return this.profilePictureUrls()[friend.id] ?? this.directProfilePictureUrl(friend.profilePicture);
  }

  markProfilePictureFailed(friend: Friend): void {
    this.failedProfilePictures.update(failedIds => {
      const nextFailedIds = new Set(failedIds);
      nextFailedIds.add(friend.id);
      return nextFailedIds;
    });
  }

  getInitial(friend: Friend): string {
    return (friend.name?.charAt(0) || friend.username.charAt(0)).toUpperCase();
  }

  private loadProfilePictures(friends: Friend[]): void {
    const activeIds = new Set(friends.map(friend => friend.id));
    this.profilePictureUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([id, url]) => {
        if (activeIds.has(id)) {
          nextUrls[id] = url;
        }
      });
      return nextUrls;
    });

    friends.forEach(friend => {
      if (!friend.profilePicture) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(friend.profilePicture, 'image', 'high').subscribe({
        next: url => {
          if (url) {
            this.profilePictureUrls.update(urls => ({ ...urls, [friend.id]: url }));
          }
        },
        error: () => this.markProfilePictureFailed(friend),
      });
    });
  }

  private directProfilePictureUrl(url: string | null): string | null {
    if (!url || this.mediaCacheService.isBackendManagedMediaUrl(url)) {
      return null;
    }

    return url;
  }
}
