import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import {Noir} from "../app-theme";
import {provideHttpClient, withInterceptors} from "@angular/common/http";
import {authInterceptor, errorInterceptor} from "./core/interceptors";
import {MessageService} from "primeng/api";

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    ),
    providePrimeNG({
      theme: {
        preset: Noir
      }
    }),
    MessageService
  ]
};
