import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, interval, startWith, switchMap } from 'rxjs';

import { DashboardFilter } from './dashboard-filter.model';
import { DashboardSummary } from './dashboard-summary.model';
import { DashboardService } from './dashboard.service';
import { FraudResult } from './fraud-result.model';

type BooleanFilter = '' | 'true' | 'false';

interface MetricCard {
  readonly label: string;
  readonly value: number;
  readonly helper: string;
  readonly tone: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);

  readonly filterForm = this.formBuilder.nonNullable.group({
    predictedFraud: ['' as BooleanFilter],
    realFraud: ['' as BooleanFilter],
    minProbability: [''],
    riskLevel: [''],
    modelVersion: [''],
    startDate: [''],
    endDate: ['']
  });

  summary?: DashboardSummary;
  results: readonly FraudResult[] = [];
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  lastUpdated?: Date;
  loadingSummary = true;
  loadingResults = true;
  summaryError = false;
  resultsError = false;

  ngOnInit(): void {
    interval(60_000)
      .pipe(
        startWith(0),
        switchMap(() => {
          this.loadingSummary = true;
          return this.dashboardService.getSummary().pipe(
            catchError(() => {
              this.summaryError = true;
              return EMPTY;
            }),
            finalize(() => this.loadingSummary = false)
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(summary => {
        this.summary = summary;
        this.summaryError = false;
        this.lastUpdated = new Date();
      });

    this.loadResults();
  }

  get metrics(): readonly MetricCard[] {
    const data = this.summary;
    return [
      { label: 'Bronze', value: data?.totalBronze ?? 0, helper: 'Linhas recebidas', tone: 'bronze' },
      { label: 'Silver', value: data?.totalSilver ?? 0, helper: 'Sinistros normalizados', tone: 'silver' },
      { label: 'Gold', value: data?.totalGold ?? 0, helper: 'Análises concluídas', tone: 'gold' },
      { label: 'Rejeitados', value: data?.totalRejected ?? 0, helper: 'Exigem correção', tone: 'danger' }
    ];
  }

  get status(): string {
    if (this.loadingSummary) {
      return 'Sincronizando';
    }
    if (this.summaryError) {
      return 'Backend indisponível';
    }
    if ((this.summary?.totalRejected ?? 0) > 0) {
      return 'Atenção necessária';
    }
    return (this.summary?.totalGold ?? 0) > 0 ? 'Operação saudável' : 'Aguardando análises';
  }

  get riskTotal(): number {
    return (this.summary?.lowRisk ?? 0)
      + (this.summary?.mediumRisk ?? 0)
      + (this.summary?.highRisk ?? 0);
  }

  refresh(): void {
    this.loadSummary();
    this.loadResults();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadResults();
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.currentPage = 0;
    this.loadResults();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
      this.loadResults();
    }
  }

  riskPercentage(value: number): number {
    return this.riskTotal === 0 ? 0 : (value / this.riskTotal) * 100;
  }

  probability(result: FraudResult): number {
    return Math.min(Math.max(result.probability * 100, 0), 100);
  }

  booleanLabel(value: boolean | null): string {
    return value === null ? 'Não informado' : value ? 'Sim' : 'Não';
  }

  riskLabel(value: string): string {
    return ({ LOW: 'Baixo', MEDIUM: 'Médio', HIGH: 'Alto' } as Record<string, string>)[value] ?? value;
  }

  private loadSummary(): void {
    this.loadingSummary = true;
    this.dashboardService.getSummary()
      .pipe(
        finalize(() => this.loadingSummary = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: summary => {
          this.summary = summary;
          this.summaryError = false;
          this.lastUpdated = new Date();
        },
        error: () => this.summaryError = true
      });
  }

  private loadResults(): void {
    this.loadingResults = true;
    this.resultsError = false;
    this.dashboardService.getResults(this.buildFilter(), this.currentPage, this.pageSize)
      .pipe(
        finalize(() => this.loadingResults = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          this.results = response.content;
          this.currentPage = response.page;
          this.pageSize = response.size;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
        },
        error: () => this.resultsError = true
      });
  }

  private buildFilter(): DashboardFilter {
    const value = this.filterForm.getRawValue();
    const probability = Number(value.minProbability);
    return {
      predictedFraud: this.toBoolean(value.predictedFraud),
      realFraud: this.toBoolean(value.realFraud),
      minProbability: value.minProbability === '' ? undefined : probability / 100,
      riskLevel: value.riskLevel || undefined,
      modelVersion: value.modelVersion.trim() || undefined,
      startDate: value.startDate || undefined,
      endDate: value.endDate || undefined
    };
  }

  private toBoolean(value: BooleanFilter): boolean | undefined {
    return value === '' ? undefined : value === 'true';
  }
}
