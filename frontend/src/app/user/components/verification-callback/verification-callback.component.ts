import {Component, inject, signal} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {UserService} from "../../user.service";
import {NotificationService} from "../../../core/services/notification.service";
import {Button} from "primeng/button";
import {ProgressSpinner} from "primeng/progressspinner";

@Component({
  selector: 'app-verification-callback',
  imports: [
    Button,
    ProgressSpinner
  ],
  templateUrl: './verification-callback.component.html',
  styleUrl: './verification-callback.component.scss',
})
export class VerificationCallbackComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private notificationService = inject(NotificationService);

  protected isError = signal<boolean>(false);

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');

    if (!code) {
      this.notificationService.showError('Verification failed: No authorization code received.');
      this.isError.set(true);
      return;
    }

    this.userService.verifyUser(code).subscribe(res => {
        this.goHome();
    });
  }

  protected goHome(): void {
    this.router.navigate(['/profile']);
  }
}
