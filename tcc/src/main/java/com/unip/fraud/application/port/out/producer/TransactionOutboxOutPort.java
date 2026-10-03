package com.unip.fraud.application.port.out.producer;

import com.unip.fraud.application.domain.PendingTransactionEvent;
import com.unip.fraud.application.domain.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionOutboxOutPort {
  void enqueue(final Transaction transaction);

  List<PendingTransactionEvent> findPending(final int limit);

  void markPublished(final UUID eventId);

  void markFailed(final UUID eventId, final String error);
}
