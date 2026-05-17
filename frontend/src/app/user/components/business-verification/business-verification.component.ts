import { Component, inject, OnInit, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { AdminService } from '../../admin.service';
import { BusinessVerification, VerificationStatus } from '../../dto/BusinessVerification';
import { Router } from '@angular/router';
import { MediaCacheService } from '../../../core/services/media-cache.service';

@Component({
  selector: 'app-business-verification',
  imports: [Button],
  templateUrl: './business-verification.component.html',
  styleUrl: './business-verification.component.scss',
})
export class BusinessVerificationComponent implements OnInit {
  private adminService = inject(AdminService);
  private mediaCacheService = inject(MediaCacheService);
  pendingVerifications = signal<BusinessVerification[]>([]);
  profilePictureUrls = signal<Record<string, string>>({});
  failedProfilePictures = signal<Set<string>>(new Set());
  loading = signal(false);
  private router = inject(Router);

  ngOnInit() {
    this.loadPendingVerifications();
  }

  private loadPendingVerifications() {
    this.loading.set(true);
    this.adminService.getPendingVerifications().subscribe({
      next: (data) => {
        this.pendingVerifications.set(data);
        this.loadProfilePictures(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  approveAccount(verification: BusinessVerification) {
    this.loading.set(true);
    this.adminService.reviewRequest(VerificationStatus.APPROVED, verification.id).subscribe({
        next: () => {
          this.loadPendingVerifications();

        }
      }
    );

  }

  denyAccount(verification: BusinessVerification) {
    this.loading.set(true);
    this.adminService.reviewRequest(VerificationStatus.REJECTED, verification.id).subscribe({
        next: () => {
          this.loadPendingVerifications();
        }
      }
    );
  }

  navigateToProfile(username: string) {
    this.router.navigate(['/profile', username]);
  }

  hasProfilePicture(verification: BusinessVerification): boolean {
    return Boolean(this.profilePictureUrl(verification))
      && !this.failedProfilePictures().has(verification.user.id);
  }

  profilePictureUrl(verification: BusinessVerification): string | null {
    return this.profilePictureUrls()[verification.user.id]
      ?? this.directProfilePictureUrl(verification.user.profilePicture);
  }

  markProfilePictureFailed(verification: BusinessVerification): void {
    this.failedProfilePictures.update(failedIds => {
      const nextFailedIds = new Set(failedIds);
      nextFailedIds.add(verification.user.id);
      return nextFailedIds;
    });
  }

  getInitial(verification: BusinessVerification): string {
    const user = verification.user;
    return (user.name?.charAt(0) || user.username.charAt(0)).toUpperCase();
  }

  private loadProfilePictures(verifications: BusinessVerification[]): void {
    const activeIds = new Set(verifications.map(verification => verification.user.id));
    this.profilePictureUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([id, url]) => {
        if (activeIds.has(id)) {
          nextUrls[id] = url;
        }
      });
      return nextUrls;
    });

    verifications.forEach(verification => {
      const profilePicture = verification.user.profilePicture;
      if (!profilePicture) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(profilePicture, 'image', 'high').subscribe({
        next: url => {
          if (url) {
            this.profilePictureUrls.update(urls => ({
              ...urls,
              [verification.user.id]: url,
            }));
          }
        },
        error: () => this.markProfilePictureFailed(verification),
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
