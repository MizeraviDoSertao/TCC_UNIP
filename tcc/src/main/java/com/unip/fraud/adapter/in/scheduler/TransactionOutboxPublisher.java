package com.unip.fraud.adapter.in.scheduler;

import com.unip.fraud.application.domain.PendingTransactionEvent;
import com.unip.fraud.application.port.out.producer.TransactionOutboxOutPort;
import com.unip.fraud.application.port.out.producer.TransactionProducerOutPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionOutboxPublisher {

  private static final Logger log = LoggerFactory.getLogger(TransactionOutboxPublisher.class);

  private final TransactionOutboxOutPort outbox;
  private final TransactionProducerOutPort producer;

  public TransactionOutboxPublisher(
      final TransactionOutboxOutPort outbox,
      final TransactionProducerOutPort producer) {
    this.outbox = outbox;
    this.producer = producer;
  }

  @Scheduled(fixedDelayString = "${outbox.publish-delay-ms:1000}")
  @Transactional
  public void publishPending() {
    outbox.findPending(100).forEach(this::publish);
  }

  private void publish(final PendingTransactionEvent event) {
    try {
      producer.send(event.transaction());
      outbox.markPublished(event.eventId());
    } catch (Exception exception) {
      log.warn("Could not publish outbox event {}", event.eventId(), exception);
      outbox.markFailed(event.eventId(), exception.getMessage());
    }
  }
}
