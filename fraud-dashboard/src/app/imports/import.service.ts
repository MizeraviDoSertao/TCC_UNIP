import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PagedResponse } from '../../page/paged-response.model';
import { ImportJob, RejectedRecord } from './import.model';

const API_ROOT = '/process_fraud_automotive/imports';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 20): Observable<PagedResponse<ImportJob>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<ImportJob>>(API_ROOT, { params });
  }

  upload(file: File): Observable<ImportJob> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<ImportJob>(API_ROOT, body);
  }

  retry(importId: string): Observable<ImportJob> {
    return this.http.post<ImportJob>(`${API_ROOT}/${encodeURIComponent(importId)}/retry`, null);
  }

  errors(importId: string, page = 0, size = 20): Observable<PagedResponse<RejectedRecord>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<RejectedRecord>>(
      `${API_ROOT}/${encodeURIComponent(importId)}/errors`,
      { params }
    );
  }
}
