import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import {LoginComponent} from "./auth/components/login/login.component";
import {RegisterComponent} from "./auth/components/registration/register.component";
import {FeedComponent} from './feed/components/feed/feed.component';
import {ProfileComponent} from "./user/components/profile/profile.component";
import {VerificationCallbackComponent} from "./user/components/verification-callback/verification-callback.component";
import {EmailVerificationComponent} from './auth/components/email-verification/email-verification.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'feed',
    loadComponent: () =>
      import('./feed/components/feed/feed.component').then(m => m.FeedComponent),
  },
  {
    path: 'post',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./post/post-page.component').then(m => m.PostPageComponent),
  },
  {path: 'feed', component: FeedComponent},
  {path: 'profile', component: ProfileComponent},
  { path: 'email_verification/:username/:email', component: EmailVerificationComponent },
  {path: 'verification-callback', component: VerificationCallbackComponent},
  {
    path: 'followed',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./user/components/followed-page/followed-page.component').then(
        m => m.FollowedPageComponent,
      ),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./user/components/profile/profile.component').then(
        m => m.ProfileComponent,
      ),
  },
  {
    path: 'verification-callback',
    loadComponent: () =>
      import('./user/components/verification-callback/verification-callback.component').then(
        m => m.VerificationCallbackComponent,
      ),
  },
];
