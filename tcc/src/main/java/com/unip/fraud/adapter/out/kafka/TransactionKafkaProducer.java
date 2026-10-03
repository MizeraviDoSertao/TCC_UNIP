package com.unip.fraud.adapter.out.kafka;

import com.unip.fraud.application.domain.Transaction;
import com.unip.fraud.application.port.out.producer.TransactionProducerOutPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TransactionKafkaProducer implements TransactionProducerOutPort {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public TransactionKafkaProducer(
      final KafkaTemplate<String, String> kafkaTemplate,
      final ObjectMapper objectMapper,
      @Value("${fraud.kafka.transactions-topic}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  @Override
  public void send(final Transaction transaction) {
    try {
      final Map<String, Object> event = new LinkedHashMap<>();
      event.put("transactionId", transaction.transactionId());
      event.put("realFraud", transaction.realFraud());
      event.put("features", transaction.features());

      final String json = objectMapper.writeValueAsString(event);
      kafkaTemplate
          .send(topic, transaction.transactionId(), json)
          .get(30, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Kafka publication was interrupted", exception);
    } catch (Exception exception) {
      throw new IllegalStateException("Error sending transaction to Kafka", exception);
    }
  }
}
