import {Component, inject, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {UserService} from "./user/user.service";
import {Toast} from "primeng/toast";
import {SidebarComponent} from './sidebar/sidebar.component';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast, SidebarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  protected userService = inject(UserService);

  protected readonly title = signal('frontend');

  ngOnInit() {
    this.userService.loadUser().subscribe();
  }
}
