import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { TabsModule } from 'primeng/tabs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-search-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    TabsModule,
    ButtonModule,
    DialogModule,
  ],
  template: `
    <div class="search-wrapper">
      <div class="tabs-container">
        <p-tabs [value]="activeTab()" (valueChange)="onTabChange($event)">
          <p-tablist>
            <p-tab value="posts" routerLink="/search/posts">
              <i class="pi pi-file"></i> Posts
            </p-tab>
            <p-tab value="users" routerLink="/search/users">
              <i class="pi pi-users"></i> Users
            </p-tab>
          </p-tablist>
        </p-tabs>
     </div>

      <!-- Router outlet for child components -->
      <div class="search-content">
        <router-outlet />
      </div>
    </div>
  `,
  styles: [`
    :host ::ng-deep .p-tablist-tab-list {
      justify-content: center;
    }

    .search-wrapper {
      max-width: 1040px;
      margin: 0 auto;
      padding: 0 1rem;
    }

    .tabs-container {
      position: relative;
    }

    .tablist-actions {
      position: absolute;
      top: 0;
      right: 0;
      height: 48px;
      display: flex;
      align-items: center;
      padding-right: 4px;
      z-index: 1;
    }

    .search-content {
      min-height: 400px;
      padding-top: 1rem;
    }

    ::ng-deep .p-tab {
      padding: 0.75rem 1.25rem;
      font-weight: 500;
    }

    ::ng-deep .p-tab i {
      margin-right: 0.5rem;
    }
  `],
})

export class SearchShellComponent {
  activeTab = signal<string>('posts');

  private router = inject(Router);

  constructor() {
    // Set initial tab based on current URL
    this.updateActiveTabFromUrl(this.router.url);

    // Listen for navigation changes
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.updateActiveTabFromUrl(event.url);
    });
  }

  private updateActiveTabFromUrl(url: string): void {
    if (url.includes('/search/users')) {
      this.activeTab.set('users');
    } else {
      this.activeTab.set('posts');
    }
  }

  onTabChange(tab: string | number | undefined): void {
    if (tab) {
      this.activeTab.set(String(tab));
    }
  }

}
