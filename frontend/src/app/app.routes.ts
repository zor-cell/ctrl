import { Routes } from '@angular/router';
import {LoginComponent} from "./auth/components/login/login.component";
import {RegisterComponent} from "./auth/components/registration/register.component";
import { SidebarComponent } from './sidebar/sidebar.component';
import {ProfileComponent} from "./user/components/profile/profile.component";
import {VerificationCallbackComponent} from "./user/components/verification-callback/verification-callback.component";
import {EmailVerificationComponent} from './auth/components/email-verification/email-verification.component';

export const routes: Routes = [
  {path: '', redirectTo: 'login', pathMatch: 'full'},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'email_verification', component: EmailVerificationComponent},
  {path: 'profile', component: ProfileComponent},
  {path: 'verification-callback', component: VerificationCallbackComponent},
  {path: '', component: SidebarComponent}
];
