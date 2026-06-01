import {Component, inject} from '@angular/core';
import {Dialog} from "primeng/dialog";
import {Button} from "primeng/button";
import {SessionStateService} from "../../session-state.service";

@Component({
  selector: 'app-limit-popup',
  imports: [
    Dialog,
    Button
  ],
  templateUrl: './limit-popup.component.html',
  styleUrl: './limit-popup.component.scss',
})
export class LimitPopupComponent {
  protected sessionState = inject(SessionStateService);
}
