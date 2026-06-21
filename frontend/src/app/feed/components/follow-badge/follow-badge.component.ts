import {ButtonModule} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {Component, computed, inject, input, signal} from '@angular/core';
import {RelationshipService} from '../../../user/relationship.service';
import {UserService} from '../../../user/user.service';
import {firstValueFrom, switchMap} from 'rxjs';
import {AuthService} from '../../../auth/auth.service';
import {toObservable, toSignal} from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-follow-badge',
  standalone: true,
  imports: [ButtonModule, Dialog],
  templateUrl: './follow-badge.component.html',
  styleUrl: './follow-badge.component.scss',
})
export class FollowBadgeComponent {
  readonly username = input.required<string>();
  readonly isExternal = input(false);
  readonly appearance = input<'compact' | 'action'>('compact');

  private readonly rel = inject(RelationshipService);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  readonly busy = signal(false);
  readonly unfriendDialogVisible = signal(false);

  readonly isNotVerifiedAndUserAdmin = computed(() => {
    const isAdmin = this.authService.isAdmin();
    const profile = this.publicProfile();
    const isVerified = profile ? profile.isVerified : true;
    return isAdmin && !isVerified;
  });

  readonly hide = computed(() =>
    this.isExternal() ||
    this.username() === this.userService.user()?.username ||
    this.isNotVerifiedAndUserAdmin()
  );

  private readonly publicProfile = toSignal(
    toObservable(this.username).pipe(
      switchMap(name => this.userService.getPublicProfile(name))
    ),
    { initialValue: null }
  );

  requestUnfriend(): void {
    this.unfriendDialogVisible.set(true);
  }

  cancelUnfriend(): void {
    if (!this.busy()) {
      this.unfriendDialogVisible.set(false);
    }
  }

  onUnfriendDialogVisibleChange(visible: boolean): void {
    if (!visible) {
      this.cancelUnfriend();
      return;
    }
    this.unfriendDialogVisible.set(true);
  }

  async confirmUnfriend() {
    this.busy.set(true);
    try {
      await firstValueFrom(this.rel.unfriend(this.username()));
      this.rel['eventBus'].unfriend();
      this.unfriendDialogVisible.set(false);
    } finally { this.busy.set(false); }
  }

  readonly actionMode = computed(() => this.appearance() === 'action');
  readonly buttonStyleClass = computed(() =>
    this.actionMode() ? 'follow-btn follow-btn--action' : 'follow-btn'
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

}
