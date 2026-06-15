import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Dialog } from 'primeng/dialog';

@Component({
  selector: 'app-chat-confirmation-dialog',
  standalone: true,
  imports: [CommonModule, Dialog],
  templateUrl: './chat-confirmation-dialog.component.html',
  styleUrl: './chat-confirmation-dialog.component.scss',
})
export class ChatConfirmationDialogComponent {
  @Input() title = '';
  @Input() message = '';
  @Input() buttonLabel = '';
  @Input() visible = false;
  @Input() inProgress = false;

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() confirmed = new EventEmitter<void>();
}
