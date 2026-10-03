import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="app-header">
      <a class="brand" routerLink="/" aria-label="Ir para o dashboard">
        <span class="brand-mark" aria-hidden="true">F</span>
        <span>
          <strong>Fraud Insight</strong>
          <small>Monitoramento inteligente</small>
        </span>
      </a>

      <nav aria-label="Navegação principal">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
          Visão geral
        </a>
        <a routerLink="/claims" routerLinkActive="active">Sinistros</a>
        <a routerLink="/imports" routerLinkActive="active">Importações</a>
      </nav>
    </header>

    <router-outlet></router-outlet>

    <footer class="app-footer">
      <span>Fraud Insight</span>
      <span>Bronze → Silver → Gold</span>
    </footer>
  `
})
export class RootComponent {
}
