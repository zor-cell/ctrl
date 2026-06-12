import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-chat-placeholder',
  standalone: true,
  template: `
    <main class="chat-placeholder">
      <h1>Chat</h1>
      <p>This feature is coming soon.</p>
    </main>
  `,
  styles: [`
    .chat-placeholder {
      display: grid;
      gap: 0.75rem;
      padding: 2rem;
      color: #111827;
    }

    .chat-placeholder h1 {
      margin: 0;
      font-size: 2rem;
    }

    .chat-placeholder p {
      margin: 0;
      color: #4b5563;
      font-size: 1.05rem;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatPlaceholderComponent {}
