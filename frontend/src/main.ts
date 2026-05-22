import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Required by sockjs-client in some browser builds.
(globalThis as typeof globalThis & { global?: typeof globalThis }).global ??= globalThis;

async function clearLegacyServiceWorkers() {
  if (!('serviceWorker' in navigator)) {
    return;
  }

  const registrations = await navigator.serviceWorker.getRegistrations();
  await Promise.all(registrations.map(registration => registration.unregister()));

  if ('caches' in window) {
    const cacheNames = await caches.keys();
    await Promise.all(cacheNames.map(cacheName => caches.delete(cacheName)));
  }
}

void clearLegacyServiceWorkers();

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
