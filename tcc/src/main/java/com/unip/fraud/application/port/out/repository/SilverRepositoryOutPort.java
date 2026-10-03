package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.Transaction;
import com.unip.fraud.application.domain.ProcessedTransaction;

import java.util.Optional;
import java.util.UUID;

public interface SilverRepositoryOutPort {
  void save(final UUID importId, final long rowNumber, final String fileName, final Transaction transaction);

  long count();

  long countByImportId(final UUID importId);

  Optional<ProcessedTransaction> findByTransactionId(final String transactionId);

  void updateConfirmedFraud(final String transactionId, final Boolean confirmedFraud);
}
