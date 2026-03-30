import {Component, inject, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import {LoginComponent} from "./auth/components/login/login.component";
import {AuthService} from "./auth/auth.service";
import {UserService} from "./user/user.service";


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoginComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  protected userService = inject(UserService);

  protected readonly title = signal('frontend');

  ngOnInit() {
    this.userService.loadUser();
  }
}
