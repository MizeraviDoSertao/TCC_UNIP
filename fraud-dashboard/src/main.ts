import { provideHttpClient } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { RootComponent } from './app/root.component';
import { routes } from './app/app.routes';

void bootstrapApplication(RootComponent, {
  providers: [provideHttpClient(), provideRouter(routes)]
}).catch((error: unknown) => console.error(error));
