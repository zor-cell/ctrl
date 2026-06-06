import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { LastWeekDayDetailResponse, SessionIntervalDto } from '../../dto/analytics.dto';

@Component({
  selector: 'app-analytics-day-detail',
  standalone: true,
  imports: [CommonModule, ButtonModule, TooltipModule],
  templateUrl: './analytics-day-detail.component.html',
  styleUrl: './analytics-day-detail.component.scss',
})
export class AnalyticsDayDetailComponent {
  readonly dayDetail = input<LastWeekDayDetailResponse | null>(null);
  readonly goBack = output<void>();
  readonly next = output<void>();
  readonly previous = output<void>();

  sessionLeft(s: SessionIntervalDto): number {
    const d = new Date(s.startTime);
    const minutes = d.getHours() * 60 + d.getMinutes();
    return (minutes / 1440) * 100;
  }

  sessionWidth(s: SessionIntervalDto): number {
    const start = new Date(s.startTime);
    const end = new Date(s.endTime);
    const durationMs = end.getTime() - start.getTime();
    const durationMinutes = durationMs / (1000 * 60);
    return (durationMinutes / 1440) * 100;
  }

  sessionTooltip(s: SessionIntervalDto): string {
    const start = new Date(s.startTime);
    const end = new Date(s.endTime);
    const durationMinutes = Math.round(
      (end.getTime() - start.getTime()) / (1000 * 60)
    );
    const timeFmt: Intl.DateTimeFormatOptions = {
      hour: '2-digit',
      minute: '2-digit',
    };
    return `${start.toLocaleTimeString([], timeFmt)} - ${end.toLocaleTimeString(
      [],
      timeFmt
    )} (${durationMinutes} min)`;
  }
}
