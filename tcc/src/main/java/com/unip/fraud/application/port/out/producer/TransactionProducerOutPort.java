package com.unip.fraud.application.port.out.producer;

import com.unip.fraud.application.domain.Transaction;

public interface TransactionProducerOutPort {
  void send(final Transaction transaction);
}
