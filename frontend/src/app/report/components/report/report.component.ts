import {Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';

import {DialogModule} from 'primeng/dialog';
import {ReportService} from '../report.service';
import {AvatarModule} from 'primeng/avatar';
import {ButtonModule} from 'primeng/button';
import {TextareaModule} from 'primeng/textarea';
import {RadioButtonModule} from 'primeng/radiobutton';
import {SelectModule} from 'primeng/select';
import {Message} from 'primeng/message';
import {ContentTypeEnum, Report} from '../../dto/report';
import {UserService} from '../../../user/user.service';
import {getReportReason, ReportReason} from '../../dto/reportReasons';

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
export class ReportComponent implements OnInit, OnChanges {
  @Input() contentType: ContentTypeEnum = ContentTypeEnum.POST;
  @Input() contentId: string | undefined;
  @Input() contentTitle: string | undefined;
  @Input() userProfilePic: string | undefined;
  @Input() username: string | undefined;
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  private fb = inject(FormBuilder);
  private reportService = inject(ReportService)
  private userService = inject(UserService);
  reportForm!: FormGroup;
  isSubmitting = signal(false);
  currentStep = signal(1)
  protected currentUser = this.userService.user;

  private reportData: Report | null = null;

  readonly ContentTypeEnum = ContentTypeEnum;

  reportReasons = signal<ReportReason[]>([]);
  ngOnInit() {
    this.initializeForm();
    this.userService.loadUser();
    this.updateReasonsByContentType();

  }

  ngOnChanges() {
    this.updateReasonsByContentType();
  }

  private updateReasonsByContentType():void {
    this.reportReasons.set(getReportReason(this.contentType));
  }

  private initializeForm(): void {
    this.reportForm = this.fb.group({
      reason: ['', Validators.required],
      description: ['', Validators.required],
    });
  }

  getContentTypeLabel(): string {
    const labels: Record<ContentTypeEnum, string> = {
      [ContentTypeEnum.POST]: 'Post',
      [ContentTypeEnum.COMMENT]: 'Comment',
      [ContentTypeEnum.ACCOUNT]: 'Account',
      [ContentTypeEnum.SOURCE]: 'Source',
    };
    return labels[this.contentType];
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
    if (!this.reportForm.valid || !this.contentId) return;

    if (!this.userService.user()?.username) {
      console.error('User not authenticated');
      return;
    }

    this.isSubmitting.set(true);
    const formData = this.reportForm.value;

    this.reportData = {
      reason: formData.reason,
      description: formData.description,
      contentType: this.contentType,
      contentId: this.contentId,
      reportingUser: this.currentUser()?.username || "",
    };

    console.log('Report submitted:', {
      contentId: this.contentId,
      contentType: this.contentType,
      reason: formData.reason,
      description: formData.description,
    });

    this.reportService.createReport(this.reportData).subscribe({
      next: () => {
        setTimeout(() => {
          this.close();
          this.isSubmitting.set(false);
        }, 500);      },
      error: (error) => {
        console.error('Error submitting report:', error);
        this.isSubmitting.set(false);
      }
    });


  }

  private resetForm(): void {
    this.reportForm.reset();
    this.currentStep.set(1);
  }
}
