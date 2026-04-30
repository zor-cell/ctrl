import {Component, Input, Output, EventEmitter, signal, inject, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Post } from '../../../feed/dto/Post';

import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { Message } from 'primeng/message';

@Component({
  selector: 'app-report',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    AvatarModule,
    ButtonModule,
    TextareaModule,
    RadioButtonModule,
    SelectModule,
    Message,
  ],
  templateUrl: './report.component.html',
  styleUrl: './report.component.scss',
})
export class ReportComponent implements OnInit{
  @Input() post: Post | null = null;
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  private fb = inject(FormBuilder);
  reportForm!: FormGroup;
  isSubmitting = signal(false);
  currentStep = signal(1);

  readonly reportReasons = [
    { label: 'Bullying or Harassment', value: 'bullying' },
    { label: 'Self harm', value: 'self_harm' },
    { label: 'Violence', value: 'violence' },
    { label: 'Illegal or Restricted items', value: 'illegal' },
    { label: 'Nudity or sexual content', value: 'nudity' },
    { label: 'Scam or Fraud', value: 'copyright' },
    { label: 'Misinformation', value: 'misinformation' },
    { label: 'Child abuse, exploitation or endangerment', value: 'child_exploitation' },
    { label: 'Non-consensual intimate images', value: 'non_consensual' },
    { label: 'Hate speech or discrimination', value: 'hate_speech' },
    { label: 'Terrorism or violent extremism', value: 'terrorism' },
    { label: 'Other', value: 'other'}
  ];

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.reportForm = this.fb.group({
      reason: ['', Validators.required],
      description: ['', Validators.required],
    });
  }

  close(): void {
    this.visibleChange.emit(false);
    this.resetForm();
  }

  continueToDetails(): void {
    if (!this.reportForm.get('reason')?.valid) {
      this.reportForm.get('reason')?.markAsTouched();
      return;
    }
    this.currentStep.set(2);
  }

  goBackToReason(): void {
    this.currentStep.set(1);
  }

  submitReport(): void {
    if (!this.reportForm.valid) return;

    this.isSubmitting.set(true);
    const formData = this.reportForm.value;

    console.log('Report submitted:', {
      postId: this.post?.postId,
      reason: formData.reason,
      description: formData.description,
    });

    setTimeout(() => {
      this.close();
      this.isSubmitting.set(false);
    }, 500);
  }

  private resetForm(): void {
    this.reportForm.reset();
    this.currentStep.set(1);
  }



}
