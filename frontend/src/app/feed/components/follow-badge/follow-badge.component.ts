import {ButtonModule} from 'primeng/button';
import {Component, computed, inject, input, signal} from '@angular/core';
import {RelationshipService} from '../../../user/relationship.service';
import {UserService} from '../../../user/user.service';
import {firstValueFrom} from 'rxjs';

@Component({
  selector: 'app-follow-badge',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './follow-badge.component.html',
  styleUrl: './follow-badge.component.scss',
})
export class FollowBadgeComponent {
  readonly username = input.required<string>();
  readonly isExternal = input(false);

  private readonly rel = inject(RelationshipService);
  private readonly userService = inject(UserService);
  readonly busy = signal(false);

  readonly hide = computed(() =>
    this.isExternal() || this.username() === this.userService.user()?.username
  );

  readonly status = computed((): string => {
    const u = this.username().toLowerCase();
    if (this.rel.friends().some(f => f.username.toLowerCase() === u))           return 'FRIEND';
    if (this.rel.followed().some(f => f.username.toLowerCase() === u))          return 'FOLLOWED';
    if (this.rel.sentRequests().some(r => r.username.toLowerCase() === u))      return 'PENDING_REQUEST_SENT';
    if (this.rel.incomingRequests().some(r => r.username.toLowerCase() === u))  return 'PENDING_REQUEST_INCOMING';
    return 'NOT_FOLLOWING';
  });

  async follow() {
    this.busy.set(true);
    try {
      await firstValueFrom(this.rel.follow(this.username()));
      // EventBus triggert loadFollowed() automatisch
      this.rel['eventBus'].followed(this.username());
    } finally { this.busy.set(false); }
  }

  async unfollow() {
    this.busy.set(true);
    try {
      await firstValueFrom(this.rel.unfollow(this.username()));
      this.rel['eventBus'].unfollowed();
    } finally { this.busy.set(false); }
  }

  async cancelRequest() {
    this.busy.set(true);
    try {
      await firstValueFrom(this.rel.cancelFriendRequest(this.username()));
    } finally { this.busy.set(false); }
  }

  async acceptRequest() {
    this.busy.set(true);
    try {
      await firstValueFrom(this.rel.acceptFriendRequest(this.username()));
    } finally { this.busy.set(false); }
  }

  async unfriend() {
    if (!confirm('Remove ' + this.username() + ' as a friend?')) return;
    this.busy.set(true);
    try {
      await firstValueFrom(this.rel.unfriend(this.username()));
      this.rel['eventBus'].unfriend();
    } finally { this.busy.set(false); }
  }
}
