import { Component, inject, OnInit, signal } from '@angular/core';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { NgClass, NgTemplateOutlet } from '@angular/common';
import { timeAgo } from '../../../core/utils/date-time.utils';
import { Avatar } from 'primeng/avatar';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { Button } from 'primeng/button';
import { UserService } from '../../../user/user.service';
import { ReportedPostResponse } from '../../dto/ReportedPostResponse';
import { ReportService } from '../report.service';
import { ContentTypeEnum, ReportStatus } from '../../dto/report';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { ReportedSourceResponse } from '../../dto/ReportedSourceResponse';

@Component({
  selector: 'app-report-review',
  imports: [
    TabList,
    Tab,
    Tabs,
    TabPanels,
    TabPanel,
    NgClass,
    Avatar,
    Tag,
    Tooltip,
    Button,
    NgTemplateOutlet,
    ConfirmDialog,
  ],
  providers: [ConfirmationService],
  templateUrl: './report-review.component.html',
  styleUrl: './report-review.component.scss',
})
export class ReportReviewComponent implements OnInit {
  protected readonly timeAgo = timeAgo;
  private userService = inject(UserService);
  private reportService = inject(ReportService);
  private confirmationService = inject(ConfirmationService);

  //protected allReports = signal<ReportedPostResponse[]>([]);
  protected reportedPosts = signal<ReportedPostResponse[]>([]);
  protected reportedSources = signal<ReportedSourceResponse[]>([]);

  protected currentTab = signal<ContentTypeEnum>(ContentTypeEnum.POST);

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
    { label: 'Other', value: 'other' },
  ];

  async ngOnInit(): Promise<void> {
    this.userService.loadUser();
    await this.changeToPosts();
  }

  async changeToPosts() {
    const reports = await this.reportService.getReportedPosts();
    this.reportedPosts.set(reports);
    this.currentTab.set(ContentTypeEnum.POST);
  }

  async changeToSources() {
    const reports = await this.reportService.getReportedSource();
    this.reportedSources.set(reports);
    console.log(reports)
    this.currentTab.set(ContentTypeEnum.SOURCE);
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)$/i.test(url);
  }

  getDomain(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }

  dismissReport(reportId: string) {
    this.confirmationService.confirm({
      message: 'Are you sure you want to dismiss this report?',
      header: 'Confirm Action',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        console.log(`Dismissing report with ID: ${reportId}`);
        this.reportService.updateStatus(ReportStatus.DECLINED, reportId).subscribe(() => {
          this.update_reports(reportId);
        });
      },
      reject: () => {
        console.log(`Dismissal cancelled for report with ID: ${reportId}`);
      },
    });
  }

  private update_reports(reportId: string) {
    if (this.currentTab() === ContentTypeEnum.POST) {
      this.reportedPosts.update((reports) =>
        reports.filter((report) => report.reportId !== reportId),
      );
    } else if (this.currentTab() === ContentTypeEnum.SOURCE) {
      this.reportedSources.update((reports) =>
        reports.filter((report) => report.reportId !== reportId),
      );
    }
  }

  getReason(reason: string) {
    return this.reportReasons.filter((r) => r.value === reason)[0].label;
  }

  markActionTaken(reportId: string) {
    this.confirmationService.confirm({
      message: 'Are you sure you want to take an action against this post?',
      header: 'Confirm Action',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        console.log(`Marking action taken for report with ID: ${reportId}`);
        this.reportService.updateStatus(ReportStatus.VALIDATED, reportId).subscribe(() => {
          this.update_reports(reportId)
        });
      },
      reject: () => {
        console.log(`Action cancelled for report with ID: ${reportId}`);
      },
    });
  }
}
