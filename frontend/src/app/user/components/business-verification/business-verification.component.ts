import { Component, inject, OnInit, signal } from '@angular/core';
import { Button } from 'primeng/button';
import { AdminService } from '../../admin.service';
import { BusinessVerification, VerificationStatus } from '../../dto/BusinessVerification';
import { Router } from '@angular/router';

@Component({
  selector: 'app-business-verification',
  imports: [Button],
  templateUrl: './business-verification.component.html',
  styleUrl: './business-verification.component.scss',
})
export class BusinessVerificationComponent implements OnInit {
  private adminService = inject(AdminService);
  pendingVerifications = signal<BusinessVerification[]>([]);
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
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  approveAccount(verification: BusinessVerification) {
    this.adminService.reviewRequest(VerificationStatus.APPROVED, verification.id).subscribe({
        next: () => {
          this.loadPendingVerifications();
        }
      }
    );

  }

  denyAccount(verification: BusinessVerification) {
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
}
