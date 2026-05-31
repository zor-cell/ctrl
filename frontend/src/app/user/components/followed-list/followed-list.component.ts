import { Component, effect, inject, input, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Button } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { Router } from '@angular/router';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { usernameInitial } from '../../../core/utils/username-initial.utils';
import { Followed } from '../../dto/Relationship';

@Component({
  selector: 'app-followed-list',
  standalone: true,
  imports: [CommonModule, Button, MenuModule],
  templateUrl: './followed-list.component.html',
  styleUrl: './followed-list.component.scss',
})
export class FollowedListComponent {
  private relationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private mediaCacheService = inject(MediaCacheService);

  @ViewChild('menu') menu!: Menu;

  // Input to determine if search filter is active
  searchQuery = input<string>('');

  followed = this.relationshipService.followed;
  profilePictureUrls = signal<Record<string, string>>({});
  failedProfilePictures = signal<Set<string>>(new Set());
  selectedUsername: string | null = null;

  menuItems = [
    {
      label: 'Unfollow',
      icon: 'pi pi-user-minus',
      command: () => this.unfollow(),
    },
  ];

  constructor() {
    effect(() => {
      this.loadProfilePictures(this.followed());
    });
  }

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

  hasProfilePicture(user: Followed): boolean {
    return Boolean(this.profilePictureUrl(user)) && !this.failedProfilePictures().has(user.id);
  }

  profilePictureUrl(user: Followed): string | null {
    return this.profilePictureUrls()[user.id] ?? this.directProfilePictureUrl(user.profilePicture);
  }

  markProfilePictureFailed(user: Followed): void {
    this.failedProfilePictures.update(failedIds => {
      const nextFailedIds = new Set(failedIds);
      nextFailedIds.add(user.id);
      return nextFailedIds;
    });
  }

  getInitial(user: Followed): string {
    return usernameInitial(user.username);
  }

  private loadProfilePictures(users: Followed[]): void {
    const activeIds = new Set(users.map(user => user.id));
    this.profilePictureUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([id, url]) => {
        if (activeIds.has(id)) {
          nextUrls[id] = url;
        }
      });
      return nextUrls;
    });

    users.forEach(user => {
      if (!user.profilePicture) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(user.profilePicture, 'image', 'high').subscribe({
        next: url => {
          if (url) {
            this.profilePictureUrls.update(urls => ({ ...urls, [user.id]: url }));
          }
        },
        error: () => this.markProfilePictureFailed(user),
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
