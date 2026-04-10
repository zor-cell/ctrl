import {Component, inject} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {UserService} from "../../user.service";
import {NotificationService} from "../../../core/services/notification.service";

@Component({
  selector: 'app-verification-callback',
  imports: [],
  templateUrl: './verification-callback.component.html',
  styleUrl: './verification-callback.component.scss',
})
export class VerificationCallbackComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private notificationService = inject(NotificationService);

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');

    if (!code) {
      this.notificationService.showError('Verification failed: No authorization code received.');
      return;
    }

    this.userService.verifyUser(code).subscribe(res => {
        this.router.navigate(['/profile']);
    });
  }
}
