import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs/operators';
import { DatePipe } from '@angular/common';

// PrimeNG Imports
import { TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { FloatLabel } from "primeng/floatlabel";
import { Message } from "primeng/message";

import { TopicService } from '../../topic.service';
import { TopicResponse } from '../../dto/TopicResponse';
import { TopicCreateRequest } from '../../dto/TopicCreateRequest';
import {emojiValidator} from "../validators";

@Component({
  selector: 'app-topic-management',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TableModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    ToastModule,
    FloatLabel,
    Message,
    DatePipe
  ],
  providers: [MessageService],
  templateUrl: './create-topic.component.html',
  styleUrl: './create-topic.component.scss',
})
export class CreateTopicComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly topicService = inject(TopicService);
  private readonly messageService = inject(MessageService);

  // --- UI State Signals ---
  public topics = signal<TopicResponse[]>([]);
  public isLoading = signal<boolean>(false);
  public isDialogOpen = signal<boolean>(false);
  public isSubmitting = signal<boolean>(false);

  // --- Edit State ---
  public editingTopicId = signal<string | null>(null);
  public dialogTitle = computed(() => this.editingTopicId() ? 'Edit Topic' : 'Create Topic');

  // --- Form ---
  public topicForm = this.fb.group({
    name: ['', [Validators.required]],
    emoji: ['', [emojiValidator()]],
    description: ['']
  });

  ngOnInit(): void {
    this.loadTopics();
  }

  public loadTopics(): void {
    this.isLoading.set(true);
    this.topicService.getTopics()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (data) => this.topics.set(data),
        error: () => this.showError('Topics konnten nicht geladen werden.')
      });
  }

  // Kombinierte Methode für Create und Edit
  public openDialog(topic?: TopicResponse): void {
    this.topicForm.reset();

    if (topic) {
      this.editingTopicId.set(topic.topicId);
      this.topicForm.patchValue({
        name: topic.name,
        emoji: topic.emoji,
        description: '' // Backend sendet es nicht mit, muss leer/neu eingegeben werden für das Embedding
      });
    } else {
      this.editingTopicId.set(null);
    }

    this.isDialogOpen.set(true);
  }

  public onSubmit(): void {
    if (this.topicForm.invalid) {
      this.topicForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const request = this.topicForm.getRawValue() as TopicCreateRequest;
    const currentId = this.editingTopicId();

    // RxJS Observable basierend auf Modus auswählen
    const request$ = currentId
      ? this.topicService.updateTopic(currentId, request)
      : this.topicService.createTopic(request);

    request$
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Erfolgreich',
            detail: currentId
              ? `Das Topic '${response.name}' wurde aktualisiert.`
              : `Das Topic '${response.name}' wurde erstellt.`
          });

          this.isDialogOpen.set(false);
          this.loadTopics();
        },
        error: (err) => this.showError(err.error?.message || 'Ein Fehler ist aufgetreten.')
      });
  }

  private showError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: 'Fehler', detail });
  }
}