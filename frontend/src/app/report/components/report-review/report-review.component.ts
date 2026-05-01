import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import {NgClass, NgTemplateOutlet} from '@angular/common';
import { timeAgo } from '../../../core/utils/date-time.utils';
import { Avatar } from 'primeng/avatar';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { Button } from 'primeng/button';
import { UserService } from '../../../user/user.service';
import { ReportedPostResponse } from '../../dto/ReportedPostResponse';
import { ReportService } from '../report.service';
import { ContentTypeEnum } from '../../dto/report';

@Component({
  selector: 'app-report-review',
  imports: [TabList, Tab, Tabs, TabPanels, TabPanel, NgClass, Avatar, Tag, Tooltip, Button, NgTemplateOutlet],
  templateUrl: './report-review.component.html',
  styleUrl: './report-review.component.scss',
})
export class ReportReviewComponent implements OnInit {
  protected readonly timeAgo = timeAgo;
  private userService = inject(UserService);
  private reportService = inject(ReportService);

  protected allReports = signal<ReportedPostResponse[]>([]);
  protected reportedPosts = computed(() =>
    this.allReports().filter((p) => p.contentType == ContentTypeEnum.POST),
  );

  async ngOnInit(): Promise<void> {
    this.userService.loadUser();
    const reports = await this.reportService.getReportedPosts();
    this.allReports.set(reports);
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

  dismissReport(reportId: any) {
    console.log(`Dismissing report with ID: ${reportId}`);
  }

  markActionTaken(reportId: any) {
    console.log(`Marking action taken for report with ID: ${reportId}`);
  }

  getVideoImageRef(mediaUrl: string | null): string[] {
    return mediaUrl ? [mediaUrl] : [];
  }
}
