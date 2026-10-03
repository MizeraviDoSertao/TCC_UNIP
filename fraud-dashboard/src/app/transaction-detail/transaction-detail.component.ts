import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { DashboardService } from '../dashboard.service';
import { TransactionDetail } from '../transaction-detail.model';

@Component({
  selector: 'app-transaction-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-detail.component.html',
  styleUrl: './transaction-detail.component.css'
})
export class TransactionDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  readonly featureSearch = new FormControl('', { nonNullable: true });
  transaction?: TransactionDetail;
  loading = true;
  notFound = false;

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap(params => this.dashboardService.getResultDetail(params.get('transactionId') ?? '')),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: transaction => {
          this.transaction = transaction;
          this.loading = false;
        },
        error: () => {
          this.notFound = true;
          this.loading = false;
        }
      });
  }

  get featureEntries(): readonly [string, unknown][] {
    const query = this.featureSearch.value.trim().toLocaleLowerCase('pt-BR');
    return Object.entries(this.transaction?.features ?? {})
      .filter(([key, value]) => !query
        || key.toLocaleLowerCase('pt-BR').includes(query)
        || this.featureValue(value).toLocaleLowerCase('pt-BR').includes(query))
      .sort(([left], [right]) => left.localeCompare(right, 'pt-BR'));
  }

  get probabilityPercentage(): number {
    return Math.min(Math.max((this.transaction?.probability ?? 0) * 100, 0), 100);
  }

  fraudLabel(value: boolean | null): string {
    return value === null ? 'Não informado' : value ? 'Fraude' : 'Legítima';
  }

  riskLabel(value: string): string {
    return ({ LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' } as Record<string, string>)[value] ?? value;
  }

  featureLabel(key: string): string {
    return key
      .replace(/_/g, ' ')
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/^./, letter => letter.toLocaleUpperCase('pt-BR'));
  }

  featureValue(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return 'Não informado';
    }
    if (typeof value === 'boolean') {
      return value ? 'Sim' : 'Não';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return String(value);
  }
}
