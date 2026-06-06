import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { AverageDayDetailResponse, TimeBucketDto } from '../../dto/analytics.dto';

@Component({
  selector: 'app-analytics-bucket-detail',
  standalone: true,
  imports: [CommonModule, ButtonModule, TooltipModule],
  templateUrl: './analytics-bucket-detail.component.html',
  styleUrl: './analytics-bucket-detail.component.scss',
})
export class AnalyticsBucketDetailComponent {
  readonly dayDetail = input<AverageDayDetailResponse | null>(null);
  readonly goBack = output<void>();

  private readonly dayNames = [
    'Mon',
    'Tue',
    'Wed',
    'Thu',
    'Fri',
    'Sat',
    'Sun',
  ];

  dayOfWeekName = computed(() => {
    const dow = this.dayDetail()?.dayOfWeek;
    if (!dow || dow < 1 || dow > 7) return '';
    return this.dayNames[dow - 1];
  });

  bucketHeight(b: TimeBucketDto): number {
    const maxAvg = 15;
    return Math.min((b.avgMinutes / maxAvg) * 100, 100);
  }

  bucketTooltip(b: TimeBucketDto): string {
    return `${b.timeLabel}: ${b.avgMinutes.toFixed(1)} min avg, ${b.contributingSessions} sessions`;
  }
}
