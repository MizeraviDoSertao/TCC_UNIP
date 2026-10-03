package com.unip.fraud.adapter.in.batch;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetReaderStrategyTest {

  @TempDir
  Path temporaryDirectory;

  @Test
  void readsQuotedCsvColumnsWithoutSplittingTheirContent() throws Exception {
    final Path csv = temporaryDirectory.resolve("claims.csv");
    Files.writeString(csv, "id,description,new column\n1,\"collision, urban area\",value\n");

    try (var cursor = new CsvDatasetReaderStrategy().open(
        csv,
        UUID.randomUUID(),
        "claims.csv"
    )) {
      final var row = cursor.read();
      assertThat(row.rowNumber()).isEqualTo(2);
      assertThat(row.values())
          .containsEntry("description", "collision, urban area")
          .containsEntry("new column", "value");
      assertThat(cursor.read()).isNull();
    }
  }

  @Test
  void readsEveryExcelSheetWithItsOwnDynamicHeader() throws Exception {
    final Path excel = temporaryDirectory.resolve("claims.xlsx");
    try (var workbook = new XSSFWorkbook()) {
      var first = workbook.createSheet("Claims");
      first.createRow(0).createCell(0).setCellValue("claim id");
      first.getRow(0).createCell(1).setCellValue("amount");
      first.createRow(1).createCell(0).setCellValue("A-1");
      first.getRow(1).createCell(1).setCellValue(1250);

      var second = workbook.createSheet("Extra");
      second.createRow(0).createCell(0).setCellValue("future field");
      second.createRow(1).createCell(0).setCellValue("supported");
      try (var output = Files.newOutputStream(excel)) {
        workbook.write(output);
      }
    }

    try (var cursor = new ExcelDatasetReaderStrategy().open(
        excel,
        UUID.randomUUID(),
        "claims.xlsx"
    )) {
      final var first = cursor.read();
      final var second = cursor.read();
      assertThat(first.sheetName()).isEqualTo("Claims");
      assertThat(first.values()).containsEntry("claim id", "A-1");
      assertThat(second.sheetName()).isEqualTo("Extra");
      assertThat(second.values()).containsEntry("future field", "supported");
      assertThat(cursor.read()).isNull();
    }
  }
}
