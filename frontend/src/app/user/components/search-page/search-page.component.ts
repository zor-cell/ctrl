import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { UserSearchComponent } from '../user-search/user-search.component';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [CommonModule, UserSearchComponent],
  template: `
    <main class="search-page">
      <app-user-search source="friends" />
    </main>
  `,
  styles: [`
    .search-page {
      max-width: 70rem;
      margin: 0 auto;
      padding: 2rem 1rem;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPageComponent {}
