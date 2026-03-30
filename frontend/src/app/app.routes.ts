import { Routes } from '@angular/router';
import {LoginComponent} from "./auth/components/login/login.component";
import {RegisterComponent} from "./auth/components/registration/register.component";

export const routes: Routes = [
  {path: '', redirectTo: 'login', pathMatch: 'full'},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
];
