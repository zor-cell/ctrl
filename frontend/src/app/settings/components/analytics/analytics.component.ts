import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from '../../services/analytics.service';
import { AnalyticsChartService } from './analytics-chart.service';
import { DatasetKey, DatasetResponse } from './analytics.types';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DetailedDay } from '../../dto/analytics.dto';

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

export class AnalyticsComponent {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly analyticsChartService = inject(AnalyticsChartService);

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
    return data ? this.analyticsChartService.buildChartOptions(key, data, this.handleBarClick, this.focusedDay()) : null;
  });

  protected focusedDay = signal<number>(0);

  title = computed(() => {
    const data = this.cache()[this.activeKey()];
    if (!data || this.viewMode() !== 'detailed') return 'Usage Analytics';
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

  changeTimeMode(updateTo: TimeMode): void {
    this.timeMode.set(updateTo)

    if (this.viewMode() === 'overview') {
      return;
    }

    const offset = (new Date().getDay() - 1) % 7;
    if (this.timeMode() === 'lastweek') {
      this.focusedDay.update(f => (f - offset) % 7);
    } else {
      this.focusedDay.update(f => (f + offset) % 7);
    }
  }
}
