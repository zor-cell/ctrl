import { Injectable } from '@angular/core';
import { ChartData, ChartOptions, TooltipItem, ChartEvent, ActiveElement } from 'chart.js';
import {
  SessionIntervalDto,
  LastWeekDayDetailResponse,
  AverageDayDetailResponse,
  LastWeekDayResponse,
  AverageWeekDayResponse,
} from '../../dto/analytics.dto';
import { DatasetKey, DatasetResponse } from './analytics.types';
import { AnnotationOptions } from 'chartjs-plugin-annotation';

export interface Session {
  start: string; // "HH:mm"
  end: string;   // "HH:mm"
}

export interface OverviewItem { label: string; minutes: number; sessionCount: number; };
export interface DetailedItem { label: string; minutes: number; tooltipLabel: string; };

@Injectable({ providedIn: 'root' })
export class AnalyticsChartService {
  private getColors() {
    return {
      primary:
        getComputedStyle(document.documentElement)
          .getPropertyValue('--p-accents-indigo')
          .trim() || '#1f2937',
      text:
        getComputedStyle(document.documentElement)
          .getPropertyValue('--p-text-color')
          .trim() || '#1f2937',
      surface:
        getComputedStyle(document.documentElement)
          .getPropertyValue('--p-surface-100')
          .trim() || '#f3f4f6',
    };
  }

  private readonly dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  buildChartData(
    key: DatasetKey,
    data: DatasetResponse,
    focusedDayIndex = 0
  ): ChartData<'bar'> {
    switch (key) {
      case 'overview-lastweek':
        return this.buildBarChartData(
          (data as LastWeekDayResponse[]),
          d => d.totalMinutes,
          d => this.dayNames[(new Date(d.date).getDay() + 6) % 7]
        );
      case 'overview-average': {
        const filled = Array.from({ length: 7 }, (_, i) =>
          (data as AverageWeekDayResponse[])
            .find(d => d.dayOfWeek === i) ?? { dayOfWeek: i, avgTotalMinutes: 0, avgSessionCount: 0 }
        );
        return this.buildBarChartData(
          filled,
          d => d.avgTotalMinutes,
          d => this.dayNames[d.dayOfWeek]
        );
      }
      case 'detailed-lastweek': {
        const day = (data as LastWeekDayDetailResponse[])[focusedDayIndex];
        const sessions = day.sessions.map(s => this.mapSessionDtoToSession(s));
        return this.buildTimelineChartData(sessions);
      }
      case 'detailed-average': {
        const day = (data as AverageDayDetailResponse[])[focusedDayIndex];
        return this.buildBarChartData(
          day.buckets,
          b => b.averageTime,
          b => b.label.slice(0, 2)
        );
      }
      default:
        throw new Error(`Unsupported dataset key: ${key}`);
    }
  }

  buildChartOptions(
    key: DatasetKey,
    data: DatasetResponse,
    onBarClick?: (index: number) => void,
    focusedDayIndex = 0,
    lineHeight = 0,
  ): ChartOptions<'bar'> {
    switch (key) {
      case 'overview-lastweek':
        return this.buildOverviewChartOptions(
          (data as LastWeekDayResponse[]),
          d => d.totalMinutes,
          d => d.sessionCount,
          onBarClick!,
          0,
          lineHeight
        );
      case 'overview-average': {
        const filled = Array.from({ length: 7 }, (_, i) =>
          (data as AverageWeekDayResponse[])
            .find(d => d.dayOfWeek === i) ?? { dayOfWeek: i, avgTotalMinutes: 0, avgSessionCount: 0 }
        );
        return this.buildOverviewChartOptions(
          filled,
          d => d.avgTotalMinutes,
          d => d.avgSessionCount,
          onBarClick!,
          1,
          lineHeight
        );
      }
      case 'detailed-lastweek': {
        return this.buildTimelineChartOptions();
      }
      case 'detailed-average': {
        const day = (data as AverageDayDetailResponse[])[focusedDayIndex];
        return this.buildDetailedChartOptions(
          day.buckets,
          b => b.averageTime,
          b => `${b.label}: ${b.averageTime.toFixed(1)} min avg, ${b.contributingSessions} ${b.contributingSessions === 1 ? 'session' : 'sessions'}`
        );
      }
      default:
        throw new Error(`Unsupported dataset key: ${key}`);
    }
  }

  // ── Reusable primitives ──

  buildBarChartData<T>(
    items: T[],
    valueFn: (item: T) => number,
    labelFn: (item: T) => string
  ): ChartData<'bar'> {
    const { primary } = this.getColors();
    return {
      labels: items.map(labelFn),
      datasets: [
        {
          data: items.map(valueFn),
          backgroundColor: primary,
          borderRadius: 4,
          barPercentage: 0.7,
        },
      ],
    };
  }

  buildOverviewChartOptions<T>(
    items: T[],
    valueFn: (item: T) => number,
    countFn: (item: T) => number,
    onBarClick: (index: number) => void,
    valueDecimal: number,
    lineHeight: number,
  ): ChartOptions<'bar'> {
    return {
      clip: false,
      responsive: true,
      resizeDelay: 100,
      maintainAspectRatio: false,
      layout: {
        padding: {
          top: 0,
          bottom: 0,
          left: 60,
          right: 60
        }
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          displayColors: false,
          callbacks: {
            label: (ctx: TooltipItem<'bar'>) => {
              const idx = ctx.dataIndex;
              const item = items[idx];
              if (!item) return '';
              const minutes = valueFn(item);
              const count = countFn(item);
              return `${minutes.toFixed(valueDecimal)} min, ${count} ${count === 1 ? 'session' : 'sessions'}`;
            },
          },
        },
        annotation: {
          annotations: {
            targetLine: {
              type: 'line' as const,
              scaleID: 'y',
              value: lineHeight,
              borderColor: 'rgba(255, 99, 132, 1)',
              borderWidth: 2,
              label: {
                display: true,
                color: this.getColors().surface,
                content: 'daily limit',
                backgroundColor: 'rgba(255, 99, 132, 1)',
                position: "start",
                xAdjust: -58,
                font: {
                  size: 12
                }
              }
            } as AnnotationOptions
          }
        }
      },
      scales: {
        y: {
          display: false,
        },
      },
      onClick: (_event: ChartEvent, elements: ActiveElement[]) => {
        if (elements.length > 0) {
          const idx = elements[0].index;
          onBarClick(idx);
        }
      },
      onHover: (event: ChartEvent, chartElement: ActiveElement[]) => {
        const nativeEvent = event.native as MouseEvent | null;
        if (nativeEvent?.target instanceof HTMLElement) {
          nativeEvent.target.style.cursor =
            chartElement.length > 0 ? 'pointer' : 'default';
        }
      },
    };
  }

  buildDetailedChartOptions<T>(
    items: T[],
    valueFn: (item: T) => number,
    tooltipLabelFn: (item: T) => string
  ): ChartOptions<'bar'> {
    return {
      responsive: true,
      resizeDelay: 100,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          displayColors: false,
          callbacks: {
            label: (ctx: TooltipItem<'bar'>) => {
              const idx = ctx.dataIndex;
              const item = items[idx];
              if (!item) return '';
              return tooltipLabelFn(item);
            },
          },
        },
      },
      scales: {
        x: {
          ticks: {
            maxTicksLimit: 24,
            autoSkip: true
          },
        },
        y: {
          display: false,
        },
      },
    };
  }

  buildTimelineChartData(sessions: Session[]): ChartData<'bar'> {
    const { primary } = this.getColors();
    return {
      labels: [''],  // single row
      datasets: sessions.map(s => ({
        data: [[this.toDecimal(s.start), this.toDecimal(s.end)]] as [number, number][],
        backgroundColor: primary,
        borderRadius: 4,
        borderSkipped: false,
        barThickness: 40,
      })),
    };
  }

  buildTimelineChartOptions(): ChartOptions<'bar'> {
    return {
      indexAxis: 'y',
      responsive: true,
      resizeDelay: 100,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          displayColors: false,
          callbacks: {
            label: (ctx: TooltipItem<'bar'>) => {
              const raw = ctx.raw as [number, number];
              const start = raw[0];
              const end = raw[1];
              const duration = end - start;
              return `${this.decimalToTime(start)} – ${this.decimalToTime(end)} (${this.decimalToMinutes(duration)} min)`;
            },
          },
        },
      },
      scales: {
        x: {
          min: 0,
          max: 24,
          ticks: {
            stepSize: 1,
          },
        },
        y: {
          stacked: true,
          display: false,
        },
      },
    };
  }
  // ── Private helpers ──

  private mapSessionDtoToSession(dto: SessionIntervalDto): Session {
    const start = new Date(dto.startTime);
    const end = new Date(dto.endTime);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return {
      start: `${pad(start.getHours())}:${pad(start.getMinutes())}`,
      end: `${pad(end.getHours())}:${pad(end.getMinutes())}`,
    };
  }

  private toDecimal(time: string): number {
    const [h, m] = time.split(':').map(Number);
    return h + m / 60;
  }

  private decimalToTime(decimal: number): string {
    const h = Math.floor(decimal);
    const m = Math.round((decimal - h) * 60);
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
  }

  private decimalToMinutes(decimal: number): string {
    const h = Math.floor(decimal);
    const m = Math.round((decimal - h) * 60);
    const val = h * 60 + m;
    return '' + val;
  }
}
