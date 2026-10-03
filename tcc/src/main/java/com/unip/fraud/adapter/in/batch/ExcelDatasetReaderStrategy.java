package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Component
public class ExcelDatasetReaderStrategy implements DatasetReaderStrategy {

  @Override
  public boolean supports(final String fileName) {
    final String normalized = fileName.toLowerCase(Locale.ROOT);
    return normalized.endsWith(".xlsx") || normalized.endsWith(".xls");
  }

  @Override
  public DatasetRowCursor open(
      final Path file,
      final UUID importId,
      final String originalFileName) throws Exception {
    final Workbook workbook = WorkbookFactory.create(file.toFile(), null, true);
    return new ExcelCursor(workbook, importId, originalFileName);
  }

  private static final class ExcelCursor implements DatasetRowCursor {

    private final Workbook workbook;
    private final UUID importId;
    private final String fileName;
    private final DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("pt-BR"));
    private final Iterator<Sheet> sheets;
    private Iterator<Row> rows;
    private List<String> headers = List.of();
    private String sheetName;

    private ExcelCursor(
        final Workbook workbook,
        final UUID importId,
        final String fileName) {
      this.workbook = workbook;
      this.importId = importId;
      this.fileName = fileName;
      this.sheets = workbook.sheetIterator();
      moveToNextSheet();
    }

    @Override
    public DatasetRow read() {
      while (rows != null) {
        while (rows.hasNext()) {
          final Row row = rows.next();
          if (!isBlank(row)) {
            return new DatasetRow(
                importId,
                fileName,
                sheetName,
                row.getRowNum() + 1L,
                values(row)
            );
          }
        }
        if (!moveToNextSheet()) {
          return null;
        }
      }
      return null;
    }

    @Override
    public void close() throws Exception {
      workbook.close();
    }

    private boolean moveToNextSheet() {
      while (sheets.hasNext()) {
        final Sheet sheet = sheets.next();
        final Iterator<Row> candidateRows = sheet.rowIterator();
        while (candidateRows.hasNext()) {
          final Row header = candidateRows.next();
          if (isBlank(header)) {
            continue;
          }
          headers = headers(header);
          HeaderValidator.validate(headers);
          sheetName = sheet.getSheetName();
          rows = candidateRows;
          return true;
        }
      }
      rows = null;
      return false;
    }

    private List<String> headers(final Row row) {
      return IntStream.range(0, row.getLastCellNum())
          .mapToObj(index -> headerValue(row, index))
          .toList();
    }

    private String headerValue(final Row row, final int index) {
      final Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      final String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
      return value.isBlank() ? "column_" + (index + 1) : value;
    }

    private Map<String, Object> values(final Row row) {
      final Map<String, Object> result = new LinkedHashMap<>();
      IntStream.range(0, headers.size()).forEach(index -> {
        final Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        result.put(headers.get(index), cellValue(cell));
      });
      return result;
    }

    private Object cellValue(final Cell cell) {
      return switch (cell) {
        case null -> null;
        default -> switch (cell.getCellType()) {
        case NUMERIC -> DateUtil.isCellDateFormatted(cell)
            ? cell.getLocalDateTimeCellValue()
            : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
        case BOOLEAN -> cell.getBooleanCellValue();
        case BLANK -> null;
        default -> formatter.formatCellValue(cell).trim();
        };
      };
    }

    private boolean isBlank(final Row row) {
      return row == null
          || row.getFirstCellNum() < 0
          || StreamSupport.stream(row.spliterator(), false)
              .allMatch(cell -> formatter.formatCellValue(cell).isBlank());
    }
  }
}
