package com.unip.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class FraudApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FraudApplication.class, args);
    }
}
