package com.unip.fraud.config.batch;

import com.unip.fraud.adapter.in.batch.DatasetBatchWriter;
import com.unip.fraud.adapter.in.batch.DynamicDatasetItemReader;
import com.unip.fraud.adapter.in.batch.DynamicDatasetProcessor;
import com.unip.fraud.adapter.in.batch.DatasetReaderFactory;
import com.unip.fraud.application.domain.DatasetRow;
import com.unip.fraud.application.domain.ProcessingOutcome;
import com.unip.fraud.application.port.out.producer.TransactionOutboxOutPort;
import com.unip.fraud.application.port.out.repository.BronzeRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.DatasetSchemaRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.RejectedRecordRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.SilverRepositoryOutPort;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class DatasetBatchConfiguration {

  @Bean
  @StepScope
  DynamicDatasetItemReader dynamicDatasetItemReader(
      final DatasetReaderFactory readerFactory,
      @Value("#{jobParameters['filePath']}") final String filePath,
      @Value("#{jobParameters['importId']}") final String importId,
      @Value("#{jobParameters['originalFileName']}") final String originalFileName) {
    return new DynamicDatasetItemReader(
        readerFactory,
        filePath,
        importId,
        originalFileName
    );
  }

  @Bean
  DynamicDatasetProcessor dynamicDatasetProcessor(
      @Value("${imports.target-aliases}") final String targetAliases) {
    return new DynamicDatasetProcessor(
        Arrays.stream(targetAliases.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toSet())
    );
  }

  @Bean
  DatasetBatchWriter datasetBatchWriter(
      final BronzeRepositoryOutPort bronzeRepository,
      final SilverRepositoryOutPort silverRepository,
      final DatasetSchemaRepositoryOutPort schemaRepository,
      final RejectedRecordRepositoryOutPort rejectedRepository,
      final TransactionOutboxOutPort transactionOutbox) {
    return new DatasetBatchWriter(
        bronzeRepository,
        silverRepository,
        schemaRepository,
        rejectedRepository,
        transactionOutbox
    );
  }

  @Bean
  Step importDatasetStep(
      final JobRepository jobRepository,
      final PlatformTransactionManager transactionManager,
      final ItemReader<DatasetRow> dynamicDatasetItemReader,
      final DynamicDatasetProcessor dynamicDatasetProcessor,
      final DatasetBatchWriter datasetBatchWriter) {
    return new StepBuilder("importDatasetStep", jobRepository)
        .<DatasetRow, ProcessingOutcome>chunk(100)
        .transactionManager(transactionManager)
        .reader(dynamicDatasetItemReader)
        .processor(dynamicDatasetProcessor)
        .writer(datasetBatchWriter)
        .build();
  }

  @Bean
  Job datasetImportJob(
      final JobRepository jobRepository,
      final Step importDatasetStep,
      final ImportJobBatchListener listener) {
    return new JobBuilder("datasetImportJob", jobRepository)
        .listener(listener)
        .start(importDatasetStep)
        .build();
  }
}
