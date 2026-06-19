import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeFormat',
  standalone: true // Wichtig für moderne Angular-Architektur
})
export class TimeFormatPipe implements PipeTransform {
  transform(minutes: number | null | undefined): string {
    if (minutes == null || isNaN(minutes)) {
      return '0 Minutes';
    }

    if (minutes < 60) {
      return `${minutes} Minute${minutes === 1 ? '' : 's'}`;
    }

    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;

    const hourStr = `${hours} Hour${hours === 1 ? '' : 's'}`;
    const minuteStr = remainingMinutes > 0 ? ` ${remainingMinutes} Minute${remainingMinutes === 1 ? '' : 's'}` : '';

    return hourStr + minuteStr;
  }
}