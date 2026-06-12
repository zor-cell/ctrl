import { Component, inject, OnInit, signal } from '@angular/core';
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

  // --- Form ---
  public topicForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    emoji: ['', [Validators.maxLength(255)]],
    description: ['', [Validators.maxLength(255)]] // Achtung: Setzt DB-Update voraus!
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

  public openCreateDialog(): void {
    this.topicForm.reset();
    this.isDialogOpen.set(true);
  }

  public onSubmit(): void {
    if (this.topicForm.invalid) {
      this.topicForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const request = this.topicForm.getRawValue() as TopicCreateRequest;

    this.topicService.createTopic(request)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Erfolgreich',
            detail: `Das Topic '${response.name}' wurde erstellt.`
          });

          this.isDialogOpen.set(false);
          this.loadTopics(); // Daten in der Tabelle aktualisieren
        },
        error: (err) => this.showError(err.error?.message || 'Das Topic existiert bereits oder ein Fehler ist aufgetreten.')
      });
  }

  private showError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: 'Fehler', detail });
  }
}