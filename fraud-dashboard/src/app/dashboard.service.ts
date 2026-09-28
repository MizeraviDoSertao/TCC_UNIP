import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

import { DashboardSummary } from './dashboard-summary.model';
import { FraudResult } from './fraud-result.model';
import { PagedResponse } from '../page/paged-response.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly summaryUrl = 'process_fraud_automotive/dashboard/summary';
  private readonly resultsUrl = 'process_fraud_automotive/dashboard/results';

  constructor(private readonly http: HttpClient) {}

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(this.summaryUrl).pipe(
      catchError(() => of({
        totalBronze: 0,
        totalSilver: 0,
        totalGold: 0,
        totalRejected: 0,
        realFraudsFromDataset: 0,
        predictedFraudsByAi: 0,
        predictedFraudPercentage: 0
      }))
    );
  }

  getResults(
  filters: Record<string, string>,
  page: number = 0,
  size: number = 10
): Observable<PagedResponse<FraudResult>> {
  let params = new HttpParams()
    .set('page', String(page ?? 0))
    .set('size', String(size ?? 10));

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, value);
    }
  });

  return this.http.get<PagedResponse<FraudResult>>(
    this.resultsUrl,
    { params }
  );
}
}