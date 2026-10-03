package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;
import com.unip.fraud.application.domain.ProcessingOutcome;
import com.unip.fraud.application.domain.Transaction;
import com.unip.fraud.application.port.out.producer.TransactionOutboxOutPort;
import com.unip.fraud.application.port.out.repository.BronzeRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.DatasetSchemaRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.RejectedRecordRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.SilverRepositoryOutPort;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.Optional;

public class DatasetBatchWriter implements ItemWriter<ProcessingOutcome> {

  private final BronzeRepositoryOutPort bronzeRepository;
  private final SilverRepositoryOutPort silverRepository;
  private final DatasetSchemaRepositoryOutPort schemaRepository;
  private final RejectedRecordRepositoryOutPort rejectedRepository;
  private final TransactionOutboxOutPort transactionOutbox;

  public DatasetBatchWriter(
      final BronzeRepositoryOutPort bronzeRepository,
      final SilverRepositoryOutPort silverRepository,
      final DatasetSchemaRepositoryOutPort schemaRepository,
      final RejectedRecordRepositoryOutPort rejectedRepository,
      final TransactionOutboxOutPort transactionOutbox) {
    this.bronzeRepository = bronzeRepository;
    this.silverRepository = silverRepository;
    this.schemaRepository = schemaRepository;
    this.rejectedRepository = rejectedRepository;
    this.transactionOutbox = transactionOutbox;
  }

  @Override
  public void write(final Chunk<? extends ProcessingOutcome> chunk) {
    chunk.forEach(this::writeOutcome);
  }

  private void writeOutcome(final ProcessingOutcome outcome) {
    final DatasetRow source = outcome.source();
    bronzeRepository.save(
        source.importId(),
        source.fileName(),
        source.sheetName(),
        source.rowNumber(),
        source.values()
    );
    Optional.ofNullable(outcome.transaction()).ifPresentOrElse(
        transaction -> persistAccepted(outcome, transaction),
        () -> rejectedRepository.save(
            source.importId(),
            source.fileName(),
            source.rowNumber(),
            source.values(),
            outcome.error()
        )
    );
  }

  private void persistAccepted(final ProcessingOutcome outcome, final Transaction transaction) {
    final DatasetRow source = outcome.source();
    silverRepository.save(source.importId(), source.rowNumber(), source.fileName(), transaction);
    schemaRepository.register(source.importId(), source.sheetName(), outcome.columns());
    transactionOutbox.enqueue(transaction);
  }
}
