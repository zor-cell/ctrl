import { Routes } from '@angular/router';
import {LoginComponent} from "./auth/components/login/login.component";
import {RegisterComponent} from "./auth/components/registration/register.component";
import { SidebarComponent } from './sidebar/sidebar.component';
import {FeedComponent} from './feed/components/feed/feed.component';
import {ProfileComponent} from "./user/components/profile/profile.component";
import {VerificationCallbackComponent} from "./user/components/verification-callback/verification-callback.component";

export const routes: Routes = [
  {path: '', redirectTo: 'login', pathMatch: 'full'},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'feed', component: FeedComponent},
  {path: 'profile', component: ProfileComponent},
  {path: 'verification-callback', component: VerificationCallbackComponent},
  {path: '', component: SidebarComponent}
];
