import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import {Noir} from "../app-theme";
import {provideHttpClient, withInterceptors} from "@angular/common/http";
import {authInterceptor} from "./auth/auth.interceptor";

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    providePrimeNG({
      theme: {
        preset: Noir
      }
    }),
    provideHttpClient(
      withInterceptors([authInterceptor])
    )
  ]
};
