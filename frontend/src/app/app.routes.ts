import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import {LoginComponent} from "./auth/components/login/login.component";
import {RegisterComponent} from "./auth/components/registration/register.component";

export const routes: Routes = [
  {path: '', redirectTo: 'login', pathMatch: 'full'},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {
    path: 'post',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./post/post-page.component').then(m => m.PostPageComponent),
  },
];
