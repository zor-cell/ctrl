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
import { ContentTypeEnum, ReportAction, ReportStatus } from '../../dto/report';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { ReportedSourceResponse } from '../../dto/ReportedSourceResponse';
import { getReportReason } from '../../dto/reportReasons';
import { ReportSummaryResponse } from '../../dto/ReportSummaryResponse';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';

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
    TableModule,
    Dialog,
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
  protected summaryDialogVisible = signal(false);
  protected reportSummaryData = signal<ReportSummaryResponse[]>([]);

  protected actionDialogVisible = signal(false);
  protected selectedReport = signal<ReportedPostResponse | ReportedSourceResponse | null>(null);

  openActionDialog(report: ReportedPostResponse | ReportedSourceResponse) {
    this.selectedReport.set(report);
    this.actionDialogVisible.set(true);
  }
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
    console.log(reports);
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
        this.reportService
          .updateStatus(ReportStatus.DECLINED, ReportAction.NONE, reportId)
          .subscribe(() => {
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
    const reasons = getReportReason(this.currentTab());
    const found = reasons.find((r) => r.value === reason);

    return found ? found.label : reason;
  }

  async viewReportSummary(reportId: string) {
    try {
      this.reportService.getReportSummary(reportId).subscribe((data) => {
        this.reportSummaryData.set(data);
        this.summaryDialogVisible.set(true);
      });
    } catch (error) {
      console.error('Error loading report summary:', error);
    }
  }

  markActionTaken(reportId: string, action: ReportAction) {
    this.confirmationService.confirm({
      message: 'Are you sure you want to take an action against this post?',
      header: 'Confirm Action',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        console.log(`Marking action taken for report with ID: ${reportId}`);
        this.reportService.updateStatus(ReportStatus.VALIDATED, action, reportId).subscribe(() => {
          this.update_reports(reportId);
        });
      },
      reject: () => {
        console.log(`Action cancelled for report with ID: ${reportId}`);
      },
    });
  }

  protected readonly ReportAction = ReportAction;
}
