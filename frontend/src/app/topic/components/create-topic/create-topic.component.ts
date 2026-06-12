import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ToastModule } from 'primeng/toast';
import {TopicService} from "../../topic.service";
import {TopicCreateRequest} from "../../dto/TopicCreateRequest";
import {FloatLabel} from "primeng/floatlabel";
import {Message} from "primeng/message";

@Component({
  selector: 'app-create-topic',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    CardModule,
    ToastModule,
    FloatLabel,
    Message
  ],
  providers: [MessageService],
  templateUrl: './create-topic.component.html',
  styleUrl: './create-topic.component.scss',
})
export class CreateTopicComponent {
  private readonly fb = inject(FormBuilder);
  private readonly topicService = inject(TopicService);

  public topicForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    emoji: ['', [Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(255)]]
  });

  public onSubmit(): void {
    if (this.topicForm.invalid) {
      this.topicForm.markAllAsTouched();
      return;
    }

    const request = this.topicForm.getRawValue() as TopicCreateRequest;
    this.topicService.createTopic(request).subscribe();
  }
}