import {Component, effect, inject, input, signal, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Button} from 'primeng/button';
import {Menu, MenuModule} from 'primeng/menu';
import {RelationshipService} from '../../relationship.service';
import {Router} from '@angular/router';
import {NotificationService} from '../../../core/services/notification.service';
import {MediaCacheService} from '../../../core/services/media-cache.service';
import {FriendRequestEventBusService} from '../../friend-request-event-bus.service';
import {Followed} from '../../dto/Relationship';
import {usernameInitial} from '../../../core/utils/username-initial.utils';

@Component({
  selector: 'app-blocked-list',
  standalone: true,
  imports: [CommonModule, Button, MenuModule],
  templateUrl: './blocked-list.component.html',
  styleUrl: './blocked-list.component.scss',
})
export class BlockedListComponent {
  private relationshipService = inject(RelationshipService);
  private router = inject(Router);
  private eventBus = inject(FriendRequestEventBusService);
  private notificationService = inject(NotificationService);
  private mediaCacheService = inject(MediaCacheService);

  @ViewChild('menu') menu!: Menu;

  searchQuery = input<string>('');
  blocked = this.relationshipService.blocked;
  profilePictureUrls = signal<Record<string, string>>({});
  failedProfilePictures = signal<Set<string>>(new Set());
  selectedUsername: string | null = null;

  menuItems = [
    {
      label: 'Unblock',
      icon: 'pi pi-user-minus',
      command: () => this.unblock(),
    },
  ];

  constructor() {
    effect(() => {
      this.loadProfilePictures(this.blocked());
    });
  }

  openMenu(event: Event, username: string) {
    this.selectedUsername = username;
    this.menu.toggle(event);
  }

  unblock() {
    if (this.selectedUsername) {
      this.relationshipService.unblock(this.selectedUsername).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Unblocked ${this.selectedUsername}`);
          this.eventBus.unblock();
          this.relationshipService.loadBlocked().subscribe();
        },
        error: () => {
          this.notificationService.showError(`Failed to unblock ${this.selectedUsername}`);
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

