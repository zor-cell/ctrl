import { Component, inject, OnInit, signal } from '@angular/core';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { NgTemplateOutlet } from '@angular/common';
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
import { MediaCacheKind, MediaCacheService } from '../../../core/services/media-cache.service';

@Component({
  selector: 'app-report-review',
  imports: [
    TabList,
    Tab,
    Tabs,
    TabPanels,
    TabPanel,
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
  private mediaCacheService = inject(MediaCacheService);
  private confirmationService = inject(ConfirmationService);

  //protected allReports = signal<ReportedPostResponse[]>([]);
  protected reportedPosts = signal<ReportedPostResponse[]>([]);
  protected profilePictureUrls = signal<Record<string, string>>({});
  protected postMediaUrls = signal<Record<string, string>>({});
  protected failedProfilePictures = signal<Set<string>>(new Set());
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
    this.loadProfilePictures(reports);
    this.loadPostMedia(reports);
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

  profilePictureUrl(report: ReportedPostResponse): string | null {
    return this.profilePictureUrls()[report.reportId]
      ?? this.directProfilePictureUrl(report.userProfilePic);
  }

  hasProfilePicture(report: ReportedPostResponse): boolean {
    return Boolean(this.profilePictureUrl(report))
      && !this.failedProfilePictures().has(report.reportId);
  }

  markProfilePictureFailed(report: ReportedPostResponse): void {
    this.failedProfilePictures.update(failedIds => {
      const nextFailedIds = new Set(failedIds);
      nextFailedIds.add(report.reportId);
      return nextFailedIds;
    });
  }

  getInitial(report: ReportedPostResponse): string {
    return report.username.charAt(0).toUpperCase();
  }

  postMediaUrl(report: ReportedPostResponse): string | null {
    return this.postMediaUrls()[report.reportId]
      ?? this.directMediaUrl(report.mediaRef);
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

  formatContentType(value?: string | null): string {
    if (!value) {
      return '';
    }
    const lower = value.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
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
      message: 'Are you sure you want to take an action?',
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

  private loadProfilePictures(reports: ReportedPostResponse[]): void {
    const activeIds = new Set(reports.map(report => report.reportId));
    this.profilePictureUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([id, url]) => {
        if (activeIds.has(id)) {
          nextUrls[id] = url;
        }
      });
      return nextUrls;
    });

    reports.forEach(report => {
      if (!report.userProfilePic) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(report.userProfilePic, 'image', 'high').subscribe({
        next: url => {
          if (url) {
            this.profilePictureUrls.update(urls => ({
              ...urls,
              [report.reportId]: url,
            }));
          }
        },
        error: () => this.markProfilePictureFailed(report),
      });
    });
  }

  private directProfilePictureUrl(url: string | null): string | null {
    if (!url || this.mediaCacheService.isBackendManagedMediaUrl(url)) {
      return null;
    }

    return url;
  }

  private loadPostMedia(reports: ReportedPostResponse[]): void {
    const activeIds = new Set(reports.map(report => report.reportId));
    this.postMediaUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([id, url]) => {
        if (activeIds.has(id)) {
          nextUrls[id] = url;
        }
      });
      return nextUrls;
    });

    reports.forEach(report => {
      if (!report.mediaRef) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(
        report.mediaRef,
        this.mediaKind(report.mediaRef),
        'high',
      ).subscribe({
        next: url => {
          if (url) {
            this.postMediaUrls.update(urls => ({
              ...urls,
              [report.reportId]: url,
            }));
          }
        },
      });
    });
  }

  private directMediaUrl(url: string | null): string | null {
    if (!url || this.mediaCacheService.isBackendManagedMediaUrl(url)) {
      return null;
    }

    return url;
  }

  private mediaKind(url: string): MediaCacheKind {
    return /\.(mp4|webm|ogg|mov)(\?|#|$)/i.test(url) ? 'video' : 'image';
  }
}
