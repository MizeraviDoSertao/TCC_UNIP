import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, interval, startWith, switchMap } from 'rxjs';

import { DashboardService } from './dashboard.service';
import { DashboardSummary } from './dashboard-summary.model';
import { FraudResult } from './fraud-result.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  summary?: DashboardSummary;
  results: FraudResult[] = [];

  filters = {
    predictedFraud: '',
    realFraud: '',
    minProbability: ''
  };

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  lastUpdated?: Date;
  private subscription?: Subscription;

  constructor(private readonly dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.subscription = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => this.dashboardService.getSummary())
      )
      .subscribe(summary => {
        this.summary = summary;
        this.lastUpdated = new Date();
      });

    this.loadResults();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  refresh(): void {
    this.dashboardService.getSummary().subscribe(summary => {
      this.summary = summary;
      this.lastUpdated = new Date();
    });

    this.loadResults();
  }

  loadResults(): void {
  const params: Record<string, string> = {};

  if (this.filters.predictedFraud !== '') {
    params['predictedFraud'] = this.filters.predictedFraud;
  }

  if (this.filters.realFraud !== '') {
    params['realFraud'] = this.filters.realFraud;
  }

  if (this.filters.minProbability !== '') {
    params['minProbability'] = this.filters.minProbability;
  }

  const page = this.currentPage ?? 0;
  const size = this.pageSize ?? 10;

  this.dashboardService.getResults(params, page, size)
    .subscribe(response => {
      this.results = response.content;
      this.currentPage = response.page;
      this.pageSize = response.size;
      this.totalElements = response.totalElements;
      this.totalPages = response.totalPages;
    });
}

  applyFilters(): void {
    this.currentPage = 0;
    this.loadResults();
  }

  clearFilters(): void {
    this.filters = {
      predictedFraud: '',
      realFraud: '',
      minProbability: ''
    };

    this.currentPage = 0;
    this.loadResults();
  }

  nextPage(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.currentPage++;
      this.loadResults();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadResults();
    }
  }

  get fraudBarWidth(): string {
    const percentage = this.summary?.predictedFraudPercentage ?? 0;
    return `${Math.min(percentage, 100)}%`;
  }

  get status(): string {
    if (!this.summary) return 'Loading';
    if (this.summary.totalGold > 0 && this.summary.totalRejected === 0) return 'Processed successfully';
    if (this.summary.totalRejected > 0) return 'Processed with warnings';
    return 'Waiting for AI results';
  }
}