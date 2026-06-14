import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { Friend } from '../../../user/dto/Relationship';
import { Chat, ChatParticipant } from '../../models/chat.models';

@Component({
  selector: 'app-chat-members-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Dialog, IconField, InputIcon, InputText, TooltipModule],
  templateUrl: './chat-members-dialog.component.html',
  styleUrl: './chat-members-dialog.component.scss',
})
export class ChatMembersDialogComponent {
  @Input() visible = false;
  @Input() chat: Chat | null = null;
  @Input() members: ChatParticipant[] = [];
  @Input() addMemberSearch = '';
  @Input() friendsToAdd: Friend[] = [];
  @Input() maxGroupMembers = 50;
  @Input() canAddMembers = false;
  @Input() selectedAddMemberCount = 0;
  @Input() canSelectAdditionalMember = false;
  @Input() avatarUrl: (id: string) => string | null = () => null;
  @Input() initialFor: (user: ChatParticipant | Friend) => string = () => '';
  @Input() isGroupOwner: (participant: ChatParticipant, chat: Chat) => boolean = () => false;
  @Input() isFriendParticipant: (participant: ChatParticipant) => boolean = () => false;
  @Input() isBlockedParticipant: (participant: ChatParticipant | null) => boolean = () => false;
  @Input() canOpenParticipantProfile: (participant: ChatParticipant | null) => boolean = () => false;
  @Input() participantProfileTooltip: (participant: ChatParticipant) => string = () => '';
  @Input() canRemoveMember: (participant: ChatParticipant, chat: Chat) => boolean = () => false;
  @Input() isAddMemberFriendSelected: (username: string) => boolean = () => false;

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() openParticipantProfile = new EventEmitter<ChatParticipant>();
  @Output() removeMember = new EventEmitter<ChatParticipant>();
  @Output() addMemberSearchChange = new EventEmitter<string>();
  @Output() toggleAddMemberFriend = new EventEmitter<string>();
  @Output() addMembers = new EventEmitter<void>();

  protected canToggleAddMember(username: string): boolean {
    return this.isAddMemberFriendSelected(username) || this.canSelectAdditionalMember;
  }
}
