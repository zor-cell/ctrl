import { Injectable } from '@angular/core';
import { ChartData, ChartOptions, TooltipItem, ChartEvent, ActiveElement } from 'chart.js';

@Injectable({ providedIn: 'root' })
export class AnalyticsChartService {
  private getColors() {
    return {
      primary:
        getComputedStyle(document.documentElement)
          .getPropertyValue('--p-primary-500')
          .trim() || '#3b82f6',
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
          label: 'Minutes',
          data: items.map(valueFn),
          backgroundColor: primary,
          borderRadius: 4,
          barPercentage: 0.7,
        },
      ],
    };
  }

  buildChartOptions<T>(
    items: T[],
    valueFn: (item: T) => number,
    countFn: (item: T) => number,
    onBarClick: (index: number) => void
  ): ChartOptions<'bar'> {
    const { text, surface } = this.getColors();

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx: TooltipItem<'bar'>) => {
              const idx = ctx.dataIndex;
              const item = items[idx];
              if (!item) return '';
              const minutes = valueFn(item);
              const count = countFn(item);
              return `${minutes.toFixed(1)} min, ${count.toFixed(1)} sessions`;
            },
          },
        },
      },
      scales: {
        x: {
          ticks: { color: text },
          grid: { color: surface },
        },
        y: {
          ticks: { color: text },
          grid: { color: surface },
          beginAtZero: true,
          title: { display: true, text: 'Minutes', color: text },
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
}
