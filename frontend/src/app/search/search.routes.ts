import { Routes } from '@angular/router';

export const searchRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/search-shell/search-shell.component').then(m => m.SearchShellComponent),
    children: [
      { path: '', redirectTo: 'posts', pathMatch: 'full' },
      {
        path: 'posts',
        loadComponent: () =>
          import('./components/posts-search/posts-search.component').then(m => m.PostsSearchComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./components/users-search/users-search.component').then(m => m.UsersSearchComponent),
      },
    ],
  },
];
