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
import { DetailedDay } from '../../dto/analytics.dto';
import { SessionConfigService } from '../../../session/session-config.service';
import { SessionConfig } from '../../../session/dto/SessionConfig';

export type ViewMode = 'overview' | 'detailed';
export type TimeMode = 'lastweek' | 'average';

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

  viewModeOptions: { label: string; value: ViewMode }[] = [
    { label: 'Overview', value: 'overview' },
    { label: 'Detailed', value: 'detailed' },
  ];

  timeModeOptions: { label: string; value: TimeMode }[] = [
    { label: 'Last Week', value: 'lastweek' },
    { label: 'Average', value: 'average' },
  ];
  viewMode = signal<ViewMode>('overview');
  timeMode = signal<TimeMode>('lastweek');

  cache = signal<Partial<Record<DatasetKey, DatasetResponse>>>({});

  loading = signal<Partial<Record<DatasetKey, boolean>>>({});

  activeKey = computed(() => `${this.viewMode()}-${this.timeMode()}` as DatasetKey);

  chartData = computed(() => {
    const key = this.activeKey();
    const data = this.cache()[key];
    return data ? this.analyticsChartService.buildChartData(key, data, this.focusedDay()) : null;
  });

  chartOptions = computed(() => {
    const key = this.activeKey();
    const data = this.cache()[key];
    return data ? this.analyticsChartService.buildChartOptions(key, data, this.handleBarClick, this.focusedDay(), this.sessionConfig().timeLimitMinutes) : null;
  });

  protected focusedDay = signal<number>(0);

  title = computed(() => {
    if (this.activeKey() === 'overview-lastweek') return 'Last 7 Days'
    if (this.activeKey() === 'overview-average') return 'Average Week'
    const data = this.cache()[this.activeKey()];
    if (!data) return 'Usage Analytics';
    return (data as DetailedDay[])[this.focusedDay()].title;
  });

  nextDayButtonVisible = computed(() => {
    if (this.viewMode() !== 'detailed') return false;
    const data = this.cache()[this.activeKey()];
    if (!data) return false;
    const dayCount = (data as DetailedDay[]).length ?? 0;
    return this.focusedDay() < dayCount - 1;
  });

  previousDayButtonVisible = computed(() => {
    if (this.viewMode() !== 'detailed') return false;
    return this.focusedDay() > 0;
  });

  sessionConfig = signal<SessionConfig>({
    timeLimitMinutes: 120,
    postLimit: 0,
    isForcedCutoff: true
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

      if (this.cache()[key]) return;

      this.loading.update(l => ({ ...l, [key]: true }));

      this.analyticsService.get(key).subscribe(data => {
        this.cache.update(c => ({ ...c, [key]: data }));
        this.loading.update(l => ({ ...l, [key]: false }));
      });
    });
  }

  protected handleBarClick = (index: number): void => {
    this.focusedDay.set(index);
    this.viewMode.set('detailed');
  };

  protected goBack(): void {
    this.viewMode.set('overview');
  }

  protected switchTimeMode(mode: TimeMode): void {
    const offset = new Date().getDay() + 2
    this.focusedDay.update(d => mode === 'average' ? (d + 7 - offset) % 7 : (d + offset) % 7);
    this.timeMode.set(mode);
  }
}
