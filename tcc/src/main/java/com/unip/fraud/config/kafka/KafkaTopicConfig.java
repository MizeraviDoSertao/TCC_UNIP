package com.unip.fraud.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class KafkaTopicConfig {

  private final String transactionsTopic;
  private final String resultsTopic;

  public KafkaTopicConfig(
      @Value("${fraud.kafka.transactions-topic}") final String transactionsTopic,
      @Value("${fraud.kafka.results-topic}") final String resultsTopic) {
    this.transactionsTopic = transactionsTopic;
    this.resultsTopic = resultsTopic;
  }

  @Bean
  public NewTopic transactionsTopic() {
    return TopicBuilder.name(transactionsTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic fraudResultsTopic() {
    return TopicBuilder.name(resultsTopic)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  public NewTopic fraudResultsDeadLetterTopic() {
    return TopicBuilder.name(resultsTopic + ".DLT")
        .partitions(3)
        .replicas(1)
        .build();
  }
}
