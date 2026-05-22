import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'postCaptionFormat',
  standalone: true,
})
export class PostCaptionFormatPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    const blocks: string[] = [];
    const lines = value.replace(/\r\n/g, '\n').split('\n');
    let paragraphLines: string[] = [];
    let listItems: string[] = [];

    const flushParagraph = () => {
      if (paragraphLines.length === 0) {
        return;
      }

      blocks.push(`<p>${paragraphLines.map(escapeHtml).join('<br>')}</p>`);
      paragraphLines = [];
    };

    const flushList = () => {
      if (listItems.length === 0) {
        return;
      }

      blocks.push(`<ul>${listItems.map(item => `<li>${escapeHtml(item)}</li>`).join('')}</ul>`);
      listItems = [];
    };

    for (const line of lines) {
      const trimmed = line.trim();

      if (!trimmed) {
        flushParagraph();
        flushList();
        continue;
      }

      if (trimmed.startsWith('# ')) {
        flushParagraph();
        flushList();
        blocks.push(`<h2>${escapeHtml(trimmed.slice(2).trim())}</h2>`);
        continue;
      }

      if (trimmed.startsWith('## ')) {
        flushParagraph();
        flushList();
        blocks.push(`<h3>${escapeHtml(trimmed.slice(3).trim())}</h3>`);
        continue;
      }

      if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
        flushParagraph();
        listItems.push(trimmed.slice(2).trim());
        continue;
      }

      flushList();
      paragraphLines.push(line);
    }

    flushParagraph();
    flushList();

    return blocks.join('');
  }
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
