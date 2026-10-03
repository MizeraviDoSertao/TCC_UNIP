package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.mapper.BronzeEntityMapper;
import com.unip.fraud.adapter.out.persistence.repository.BronzeTransactionRawRepository;
import com.unip.fraud.application.port.out.repository.BronzeRepositoryOutPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class BronzePersistenceAdapter implements BronzeRepositoryOutPort {

  private final BronzeTransactionRawRepository bronzeTransactionRawRepository;
  private final BronzeEntityMapper mapper;

  public BronzePersistenceAdapter(
      final BronzeTransactionRawRepository bronzeTransactionRawRepository,
      final BronzeEntityMapper mapper) {
    this.bronzeTransactionRawRepository = bronzeTransactionRawRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(
      final UUID importId,
      final String fileName,
      final String sheetName,
      final long rowNumber,
      final Map<String, Object> rawPayload) {
    bronzeTransactionRawRepository.save(mapper.toEntity(
        importId,
        fileName,
        sheetName,
        rowNumber,
        rawPayload,
        LocalDateTime.now()
    ));
  }

  @Override
  public long count() {
    return bronzeTransactionRawRepository.count();
  }
}
