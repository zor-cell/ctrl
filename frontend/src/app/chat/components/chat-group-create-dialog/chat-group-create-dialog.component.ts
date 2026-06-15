import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';

import { Friend } from '../../../user/dto/Relationship';

@Component({
  selector: 'app-chat-group-create-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, Dialog, IconField, InputIcon, InputText],
  templateUrl: './chat-group-create-dialog.component.html',
  styleUrl: './chat-group-create-dialog.component.scss',
})
export class ChatGroupCreateDialogComponent {
  @Input() visible = false;
  @Input() groupName = '';
  @Input() groupMemberSearch = '';
  @Input() groupImagePreview: string | null = null;
  @Input() selectedMemberCount = 1;
  @Input() maxGroupMembers = 50;
  @Input() friends: Friend[] = [];
  @Input() groupNameValid = true;
  @Input() canCreate = false;
  @Input() avatarUrl: (id: string) => string | null = () => null;
  @Input() initialFor: (friend: Friend) => string = () => '';
  @Input() isFriendSelected: (username: string) => boolean = () => false;
  @Input() canSelectFriend: (username: string) => boolean = () => false;

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() groupNameChange = new EventEmitter<string>();
  @Output() groupMemberSearchChange = new EventEmitter<string>();
  @Output() imageSelected = new EventEmitter<Event>();
  @Output() clearImage = new EventEmitter<void>();
  @Output() toggleFriend = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() createGroup = new EventEmitter<void>();
}
