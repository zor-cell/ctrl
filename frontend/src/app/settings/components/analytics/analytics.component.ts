import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from '../../services/analytics.service';
import {
  LastWeekDayResponse,
  AverageDayResponse,
  LastWeekDayDetailResponse,
  AverageDayDetailResponse,
} from '../../dto/analytics.dto';
import { AnalyticsOverviewComponent } from './analytics-overview.component';
import { AnalyticsDayDetailComponent } from './analytics-day-detail.component';
import { AnalyticsBucketDetailComponent } from './analytics-bucket-detail.component';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule,
    AnalyticsOverviewComponent,
    AnalyticsDayDetailComponent,
    AnalyticsBucketDetailComponent,
  ],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);
  math = Math;

  protected activeTab = signal<'lastWeek' | 'average'>('lastWeek');
  protected view = signal<'overview' | 'dayDetail' | 'bucketDetail'>('overview');
  protected focusedDay = signal<number>(0);

  protected isLoadingLastWeek = signal(true);
  protected isLoadingAverage = signal(true);

  protected lastWeekData = signal<LastWeekDayResponse[]>([]);
  protected averageData = signal<AverageDayResponse[]>([]);

  protected lastWeekDayDetail = signal<LastWeekDayDetailResponse[]>([]);
  protected averageDayDetail = signal<AverageDayDetailResponse[]>([]);

  protected isLoadingLastWeekDetail = signal(false);
  protected isLoadingAverageDetail = signal(false);

  ngOnInit(): void {
    this.loadLastWeek();
    this.loadAverage();
  }

  private loadLastWeek(): void {
    this.isLoadingLastWeek.set(true);
    this.analyticsService.getLastWeek().subscribe({
      next: (res) => {
        this.lastWeekData.set(res.days);
        this.isLoadingLastWeek.set(false);
      },
      error: () => this.isLoadingLastWeek.set(false),
    });
  }

  private loadAverage(): void {
    this.isLoadingAverage.set(true);
    this.analyticsService.getWeeklyAverage().subscribe({
      next: (res) => {
        this.averageData.set(res.days);
        this.isLoadingAverage.set(false);
      },
      error: () => this.isLoadingAverage.set(false),
    });
  }

  protected onLastWeekBarClick(index: number): void {
    const date = this.lastWeekData()[index]?.date;
    if (!date) return;
    this.isLoadingLastWeekDetail.set(true);
    this.analyticsService.getDayDetail().subscribe({
      next: (res) => {
        this.lastWeekDayDetail.set(res.days);
        this.isLoadingLastWeekDetail.set(false);
        this.view.set('dayDetail');
        this.focusedDay.set(index);
      },
      error: () => this.isLoadingLastWeekDetail.set(false),
    });
  }

  protected onAverageBarClick(index: number): void {
    const dayResponse = this.averageData()[index];
    if (!dayResponse) return;
    const dayOfWeek = this.mapDayOfWeekStringToNumber(dayResponse.dayOfWeek);
    if (!dayOfWeek) return;
    this.isLoadingAverageDetail.set(true);
    this.analyticsService.getDayOfWeekDetail().subscribe({
      next: (res) => {
        this.averageDayDetail.set(res.days);
        this.isLoadingAverageDetail.set(false);
        this.view.set('bucketDetail');
        this.focusedDay.set(index);
      },
      error: () => this.isLoadingAverageDetail.set(false),
    });
  }

  protected goBack(): void {
    this.view.set('overview');
  }

  private mapDayOfWeekStringToNumber(dayOfWeek: string): number | null {
    const numeric = Number(dayOfWeek);
    if (!isNaN(numeric) && numeric >= 1 && numeric <= 7) {
      return numeric;
    }
    const map: Record<string, number> = {
      Mon: 1,
      Tue: 2,
      Wed: 3,
      Thu: 4,
      Fri: 5,
      Sat: 6,
      Sun: 7,
      Monday: 1,
      Tuesday: 2,
      Wednesday: 3,
      Thursday: 4,
      Friday: 5,
      Saturday: 6,
      Sunday: 7,
    };
    return map[dayOfWeek] ?? null;
  }
}
