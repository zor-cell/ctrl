import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from '../../analytics.service';
import { AnalyticsChartService } from './analytics-chart.service';
import { DatasetKey, DatasetResponse } from './analytics.types';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { AverageDayDetailResponse, AverageWeekDayResponse, WeekDayDetailResponse, WeekDayResponse } from '../../dto/analytics.dto';
import { SessionConfigService } from '../../../session/session-config.service';
import { SessionConfig } from '../../../session/dto/SessionConfig';
import { UserService } from '../../../user/user.service';
import { UserDetails } from '../../../user/dto/UserDetails';

export type ViewMode = 'overview' | 'detailed';
export type TimeMode = 'days' | 'average';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    CommonModule,
    Button,
    ChartModule,
    SkeletonModule,
    SelectButtonModule,
    FormsModule
  ],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})

export class AnalyticsComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly analyticsChartService = inject(AnalyticsChartService);
  private readonly configService = inject(SessionConfigService);
  private readonly userService = inject(UserService);

  viewModeOptions: { label: string; value: ViewMode }[] = [
    { label: 'Overview', value: 'overview' },
    { label: 'Detailed', value: 'detailed' },
  ];

  timeModeOptions: { label: string; value: TimeMode }[] = [
    { label: 'Last Week', value: 'days' },
    { label: 'Average', value: 'average' },
  ];
  viewMode = signal<ViewMode>('overview');
  timeMode = signal<TimeMode>('days');

  cache = signal<Partial<Record<DatasetKey, Record<number, DatasetResponse>>>>({});

  loadedWeeks = signal<Partial<Record<DatasetKey, Set<number>>>>({});

  loading = signal<Partial<Record<DatasetKey, boolean>>>({});

  activeKey = computed(() => `${this.viewMode()}-${this.timeMode()}` as DatasetKey);

  chartData = computed(() => {
    const key = this.activeKey();
    const data = this.cache()[key]?.[this.timeMode() === 'average' ? 0 : this.focusedWeek()];
    return data ? this.analyticsChartService.buildChartData(key, data, this.focusedDay()) : null;
  });

  chartOptions = computed(() => {
    const key = this.activeKey();
    const data = this.cache()[key]?.[this.timeMode() === 'average' ? 0 : this.focusedWeek()];
    return data ? this.analyticsChartService.buildChartOptions(key, data, this.handleBarClick, this.focusedDay(), this.sessionConfig().timeLimitMinutes) : null;
  });

  daysSinceAccountCreation = computed(() => {
    const createdAt = new Date((this.userService.user() as UserDetails).createdAt);
    const now = new Date();
    const msPerDay = 1000 * 60 * 60 * 24;

    const nowUtc = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    const createdUtc = Date.UTC(createdAt.getFullYear(), createdAt.getMonth(), createdAt.getDate());

    return Math.floor((nowUtc - createdUtc) / msPerDay);
  });
  protected focusedWeek = signal<number>(0);
  protected focusedDay = signal<number>(0);

  title = computed(() => {
    switch (this.activeKey()) {
      case 'overview-days':
        if (this.focusedWeek() === 0) {
          return 'This Week';
        } else if (this.focusedWeek() === 1) {
          return 'Last Week';
        } else {
          const start = (this.cache()['overview-days']?.[this.focusedWeek()].at(0) as WeekDayResponse)?.date;
          const end = (this.cache()['overview-days']?.[this.focusedWeek()].at(6) as WeekDayResponse)?.date;
          return start && end ? `${start} - ${end}` : '';
        }
      case 'overview-average': {
        const days = ((this.cache()['overview-average']?.[0] as AverageWeekDayResponse[])?.reduce((sum, item) => sum + item.daysInPeriod, 0) ?? 0);
        if (days === 0) {
          return 'No sessions to average about yet';
        }
        return 'Average Week across the last ' + days + ' days';
      }
      case 'detailed-days':
        return (this.cache()['detailed-days']?.[this.focusedWeek()].at(this.focusedDay()) as WeekDayDetailResponse)?.title ?? '';
      case 'detailed-average': {
        const detailedAverage = (this.cache()['detailed-average']?.[0].at(this.focusedDay()) as AverageDayDetailResponse);
        if (!detailedAverage) {
          return 'Usage Analytics';
        } else if (detailedAverage.daysInPeriod === 0) {
          return 'No sessions on ' + detailedAverage.title + '\'s so far!';
        } else if (detailedAverage.daysInPeriod === 1) {
          return 'Average ' + detailedAverage.title + ' across the last week';
        }
        return 'Average ' + detailedAverage.title + ' across the last ' + detailedAverage.daysInPeriod + ' weeks';
      }
    }
  });

  nextButtonVisible = computed(() => {
    switch (this.activeKey()) {
      case 'overview-days':
        return this.focusedWeek() > 0;
      case 'overview-average':
        return false;
      case 'detailed-days':
        return this.focusedWeek() > 0 || this.focusedDay() < 6;
      case 'detailed-average':
        return this.focusedDay() < 6;
    }
  });

  previousButtonVisible = computed(() => {
    switch (this.activeKey()) {
      case 'overview-days':
        return this.focusedWeek() < Math.ceil(this.daysSinceAccountCreation() / 7) - 1; //TODO: buggy for sure
      case 'overview-average':
        return false;
      case 'detailed-days':
        return this.focusedWeek() < Math.ceil(this.daysSinceAccountCreation() / 7) - 1 || this.focusedDay() > 0;
      case 'detailed-average':
        return this.focusedDay() > 0;
    }
  });

  sessionConfig = signal<SessionConfig>({
    timeLimitMinutes: 120,
    postLimit: 0,
    allowSnooze: true
  });

  ngOnInit(): void {
    this.configService.getConfig().subscribe({
      next: (config: SessionConfig) => {
        this.sessionConfig.set(config);
      },
    });
  }

  constructor() {
    effect(() => {
      const key = this.activeKey();
      const week = this.timeMode() === 'average' ? 0 : this.focusedWeek();

      const alreadyLoaded = this.loadedWeeks()[key]?.has(week);
      if (alreadyLoaded) return;

      this.loading.update(l => ({ ...l, [key]: true }));

      this.analyticsService.get(key, week).subscribe(data => {
        this.cache.update(c => ({
          ...c,
          [key]: { ...c[key], [week]: data }
        }));
        this.loadedWeeks.update(lw => ({
          ...lw,
          [key]: new Set([...(lw[key] ?? []), week])
        }));
        this.loading.update(l => ({ ...l, [key]: false }));
      });
    });
  }

  protected lastTappedIndex = signal<number | null>(null);

  protected handleBarClick = (index: number): void => {
    //user clicked somewhere but not on the bar
    if(index == -1) {
      this.lastTappedIndex.set(null);
      return;
    }

    const isTouchDevice = window.matchMedia('(max-width: 768px)').matches;
    if (isTouchDevice) {
      if (this.lastTappedIndex() === index) {
        //its the second tab -> go into detail view
        this.focusedDay.set(index);
        this.viewMode.set('detailed');
        this.lastTappedIndex.set(null);
      } else {
        //its the first tap -> let chart.js show the tooltip
        this.lastTappedIndex.set(index);
      }
    } else {
      this.focusedDay.set(index);
      this.viewMode.set('detailed');
    }
  };

  protected goBack(): void {
    this.viewMode.set('overview');
  }

  nextButtonClick(): void {
    if (this.viewMode() === 'overview') {
      this.focusedWeek.update(w => Math.max(w - 1, 0));
      return;
    }

    if (this.focusedDay() === 6) {
      this.focusedWeek.update(w => Math.max(w - 1, 0));
      this.focusedDay.set(0);
      return;
    }

    this.focusedDay.update(d => Math.min(d + 1, 6));
  }

  previousButtonClick(): void {
    if (this.viewMode() === 'overview') {
      this.focusedWeek.update(w => w + 1);
      return;
    }

    if (this.focusedDay() === 0) {
      this.focusedWeek.update(w => w + 1);
      this.focusedDay.set(6);
      return;
    }

    this.focusedDay.update(d => Math.max(d - 1, 0));
  }
}
