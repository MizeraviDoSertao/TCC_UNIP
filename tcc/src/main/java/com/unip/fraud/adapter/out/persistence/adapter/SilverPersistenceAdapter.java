package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.mapper.SilverEntityMapper;
import com.unip.fraud.adapter.out.persistence.repository.SilverTransactionRepository;
import com.unip.fraud.application.domain.ProcessedTransaction;
import com.unip.fraud.application.domain.Transaction;
import com.unip.fraud.application.port.out.repository.SilverRepositoryOutPort;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class SilverPersistenceAdapter implements SilverRepositoryOutPort {

  private final SilverTransactionRepository silverTransactionRepository;
  private final SilverEntityMapper mapper;

  public SilverPersistenceAdapter(
      final SilverTransactionRepository silverTransactionRepository,
      final SilverEntityMapper mapper) {
    this.silverTransactionRepository = silverTransactionRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(
      final UUID importId,
      final long rowNumber,
      final String fileName,
      final Transaction transaction) {
    silverTransactionRepository.save(mapper.toEntity(
        importId,
        rowNumber,
        fileName,
        transaction,
        LocalDateTime.now()
    ));
  }

  @Override
  public long count() {
    return silverTransactionRepository.count();
  }

  @Override
  public long countByImportId(final UUID importId) {
    return silverTransactionRepository.countByImportId(importId);
  }

  @Override
  public Optional<ProcessedTransaction> findByTransactionId(final String transactionId) {
    return silverTransactionRepository.findById(transactionId)
        .map(mapper::toProcessedTransaction);
  }

  @Override
  public void updateConfirmedFraud(
      final String transactionId,
      final Boolean confirmedFraud) {
    silverTransactionRepository.findById(transactionId).ifPresent(entity -> {
      entity.confirmFraud(confirmedFraud);
      silverTransactionRepository.save(entity);
    });
  }
}
