package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CsvDatasetReaderStrategy implements DatasetReaderStrategy {

  @Override
  public boolean supports(final String fileName) {
    return fileName.toLowerCase(Locale.ROOT).endsWith(".csv");
  }

  @Override
  public DatasetRowCursor open(
      final Path file,
      final UUID importId,
      final String originalFileName) throws Exception {
    final BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
    final CSVParser parser = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreEmptyLines(true)
        .setTrim(true)
        .get()
        .parse(reader);
    final List<String> headers = new ArrayList<>(parser.getHeaderNames());
    HeaderValidator.validate(headers);
    final Iterator<CSVRecord> records = parser.iterator();

    return new DatasetRowCursor() {
      @Override
      public DatasetRow read() {
        if (!records.hasNext()) {
          return null;
        }
        final CSVRecord record = records.next();
        final Map<String, Object> values = new LinkedHashMap<>();
        headers.forEach(header ->
            values.put(header, record.isMapped(header) ? record.get(header) : null)
        );
        return new DatasetRow(
            importId,
            originalFileName,
            "CSV",
            record.getRecordNumber() + 1,
            values
        );
      }

      @Override
      public void close() throws Exception {
        parser.close();
      }
    };
  }
}
