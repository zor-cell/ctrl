import {Component, inject, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {UserService} from "./user/user.service";
import {Toast} from "primeng/toast";


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast],
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
