import { Component, input, output, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { Tab, TabList, TabPanel, TabPanels, Tabs } from 'primeng/tabs';
import { NgZone } from '@angular/core';
import { AnalyticsChartService } from './analytics-chart.service';
import { LastWeekDayResponse, AverageDayResponse } from '../../dto/analytics.dto';

@Component({
  selector: 'app-analytics-overview',
  standalone: true,
  imports: [
    CommonModule,
    ChartModule,
    SkeletonModule,
    Tab,
    TabList,
    TabPanel,
    TabPanels,
    Tabs,
  ],
  templateUrl: './analytics-overview.component.html',
  styleUrl: './analytics-overview.component.scss',
})
export class AnalyticsOverviewComponent {
  private readonly chartService = inject(AnalyticsChartService);
  private readonly ngZone = inject(NgZone);

  readonly lastWeekData = input<LastWeekDayResponse[]>([]);
  readonly averageData = input<AverageDayResponse[]>([]);
  readonly isLoadingLastWeek = input<boolean>(false);
  readonly isLoadingAverage = input<boolean>(false);
  readonly activeTab = input<'lastWeek' | 'average'>('lastWeek');

  readonly tabChange = output<'lastWeek' | 'average'>();
  readonly lastWeekBarClick = output<number>();
  readonly averageBarClick = output<number>();

  lastWeekChartData = computed(() =>
    this.chartService.buildBarChartData(
      this.lastWeekData(),
      (d) => d.totalMinutes,
      (d) => d.date.slice(5)
    )
  );

  averageChartData = computed(() =>
    this.chartService.buildBarChartData(
      this.averageData(),
      (d) => d.avgTotalMinutes,
      (d) => d.dayOfWeek
    )
  );

  lastWeekChartOptions = computed(() =>
    this.chartService.buildChartOptions(
      this.lastWeekData(),
      (d) => d.totalMinutes,
      (d) => d.sessionCount,
      (idx) => this.ngZone.run(() => this.lastWeekBarClick.emit(idx))
    )
  );

  averageChartOptions = computed(() =>
    this.chartService.buildChartOptions(
      this.averageData(),
      (d) => d.avgTotalMinutes,
      (d) => d.avgSessionCount,
      (idx) => this.ngZone.run(() => this.averageBarClick.emit(idx))
    )
  );
}
