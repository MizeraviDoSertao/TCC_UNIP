import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, interval, startWith, switchMap } from 'rxjs';

import { ImportJob, ImportStatus, RejectedRecord } from './import.model';
import { ImportService } from './import.service';

@Component({
  selector: 'app-imports',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './imports.component.html',
  styleUrl: './imports.component.css'
})
export class ImportsComponent implements OnInit {
  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;

  private readonly importService = inject(ImportService);
  private readonly destroyRef = inject(DestroyRef);

  imports: readonly ImportJob[] = [];
  selectedFile?: File;
  selectedImport?: ImportJob;
  errors: readonly RejectedRecord[] = [];
  loading = true;
  uploading = false;
  loadError = false;
  uploadError = '';

  private readonly statusLabels: Readonly<Record<ImportStatus, string>> = {
    QUEUED: 'Na fila',
    PROCESSING: 'Processando',
    COMPLETED: 'Concluído',
    COMPLETED_WITH_WARNINGS: 'Concluído com alertas',
    FAILED: 'Falhou'
  };

  ngOnInit(): void {
    interval(15_000)
      .pipe(
        startWith(0),
        switchMap(() => this.importService.list(0, 50).pipe(
          catchError(() => {
            this.loadError = true;
            this.loading = false;
            return EMPTY;
          })
        )),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(response => {
        this.imports = response.content;
        this.loading = false;
        this.loadError = false;
        this.synchronizeSelection();
      });
  }

  chooseFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.item(0) ?? undefined;
    this.uploadError = '';
  }

  upload(): void {
    if (!this.selectedFile || this.uploading) {
      return;
    }
    this.uploading = true;
    this.uploadError = '';
    this.importService.upload(this.selectedFile)
      .pipe(
        finalize(() => this.uploading = false),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: job => {
          this.imports = [job, ...this.imports.filter(item => item.importId !== job.importId)];
          this.selectedImport = job;
          this.selectedFile = undefined;
          if (this.fileInput) {
            this.fileInput.nativeElement.value = '';
          }
        },
        error: error => this.uploadError = error?.error?.message
          ?? 'Não foi possível enviar o arquivo. Verifique formato, tamanho e duplicidade.'
      });
  }

  selectImport(job: ImportJob): void {
    this.selectedImport = job;
    this.errors = [];
    if (job.rejectedRows > 0) {
      this.importService.errors(job.importId, 0, 20)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: response => this.errors = response.content,
          error: () => this.errors = []
        });
    }
  }

  retry(job: ImportJob): void {
    this.importService.retry(job.importId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.imports = this.imports.map(item => item.importId === updated.importId ? updated : item);
          this.selectedImport = updated;
        },
        error: error => this.uploadError = error?.error?.message ?? 'Não foi possível reiniciar esta importação.'
      });
  }

  statusLabel(status: ImportStatus): string {
    return this.statusLabels[status];
  }

  progress(job: ImportJob): number {
    if (job.totalRows === 0) {
      return job.status === 'COMPLETED' ? 100 : 0;
    }
    return Math.min(((job.processedRows + job.rejectedRows) / job.totalRows) * 100, 100);
  }

  fileSize(file: File): string {
    const megabytes = file.size / 1024 / 1024;
    return `${megabytes.toLocaleString('pt-BR', { maximumFractionDigits: 2 })} MB`;
  }

  errorPreview(record: RejectedRecord): string {
    return Object.entries(record.originalData)
      .slice(0, 2)
      .map(([key, value]) => `${key}: ${String(value ?? '—')}`)
      .join(' · ');
  }

  private synchronizeSelection(): void {
    const selectedId = this.selectedImport?.importId;
    if (selectedId) {
      this.selectedImport = this.imports.find(item => item.importId === selectedId);
    }
  }
}
