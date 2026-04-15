import {Component, inject, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {UserService} from "./user/user.service";
import {Toast} from "primeng/toast";
import {NavigationComponent} from './navigation/navigation.component';
import {SidebarComponent} from './sidebar/sidebar.component';
import { AuthService } from './auth/auth.service';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast, NavigationComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  protected userService = inject(UserService);
  private readonly authService = inject(AuthService);

  ngOnInit() {
    if (this.authService.token()) {
      this.userService.loadUser().subscribe();
    }
  }
}
