package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;
import com.unip.fraud.application.domain.DetectedColumn;
import com.unip.fraud.application.domain.ProcessingOutcome;
import com.unip.fraud.application.domain.Transaction;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.unip.fraud.application.validation.Validation.requireArgument;

public class DynamicDatasetProcessor implements ItemProcessor<DatasetRow, ProcessingOutcome> {

  private static final Pattern INTEGER = Pattern.compile("[-+]?\\d+");
  private static final Pattern DECIMAL = Pattern.compile("[-+]?\\d+[.,]\\d+");
  private static final Pattern BRAZILIAN_MONEY = Pattern.compile(
      "(?:R\\$\\s*)?[-+]?\\d{1,3}(?:\\.\\d{3})*,\\d{2}"
  );
  private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
      DateTimeFormatter.ISO_LOCAL_DATE,
      DateTimeFormatter.ofPattern("dd/MM/uuuu"),
      DateTimeFormatter.ofPattern("dd-MM-uuuu")
  );
  private static final Map<String, Boolean> LABEL_VALUES = Map.ofEntries(
      Map.entry("fraud", true),
      Map.entry("fraude", true),
      Map.entry("yes", true),
      Map.entry("sim", true),
      Map.entry("true", true),
      Map.entry("1", true),
      Map.entry("legitimate", false),
      Map.entry("legitimo", false),
      Map.entry("legítimo", false),
      Map.entry("no", false),
      Map.entry("nao", false),
      Map.entry("não", false),
      Map.entry("false", false),
      Map.entry("0", false)
  );
  private static final List<Function<String, Optional<Object>>> VALUE_NORMALIZERS = List.of(
      DynamicDatasetProcessor::parseBooleanValue,
      DynamicDatasetProcessor::parseDateValue,
      DynamicDatasetProcessor::parseBrazilianMoney,
      DynamicDatasetProcessor::parseInteger,
      DynamicDatasetProcessor::parseDecimal
  );

  private final Set<String> labelAliases;

  public DynamicDatasetProcessor(final Set<String> labelAliases) {
    this.labelAliases = labelAliases.stream()
        .map(DynamicDatasetProcessor::normalizeName)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public ProcessingOutcome process(final DatasetRow row) {
    try {
      final List<NormalizedAttribute> attributes = row.values().entrySet().stream()
          .map(this::normalizeAttribute)
          .toList();
      validateUniqueNames(attributes);
      validateSingleLabel(attributes);

      final Map<String, Object> normalized = new LinkedHashMap<>();
      attributes.stream()
          .filter(attribute -> !attribute.label())
          .forEach(attribute -> normalized.put(attribute.normalizedName(), attribute.value()));
      requireArgument(!normalized.isEmpty(), "The row has no usable attributes");

      final Boolean confirmedFraud = attributes.stream()
          .filter(NormalizedAttribute::label)
          .findFirst()
          .map(NormalizedAttribute::value)
          .map(this::parseLabel)
          .orElse(null);
      final List<DetectedColumn> columns = attributes.stream()
          .map(attribute -> new DetectedColumn(
              attribute.originalName(),
              attribute.normalizedName(),
              typeOf(attribute.value()),
              attribute.label() ? "LABEL" : "FEATURE"
          ))
          .toList();
      final String seed = row.importId() + ":" + row.sheetName() + ":" + row.rowNumber();
      final String transactionId = UUID.nameUUIDFromBytes(
          seed.getBytes(StandardCharsets.UTF_8)
      ).toString();

      return ProcessingOutcome.accepted(
          row,
          new Transaction(transactionId, confirmedFraud, normalized),
          columns
      );
    } catch (Exception exception) {
      return ProcessingOutcome.rejected(row, exception);
    }
  }

  static String normalizeName(final String value) {
    final String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return withoutAccents.toLowerCase(Locale.ROOT)
        .trim()
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
  }

  private NormalizedAttribute normalizeAttribute(final Map.Entry<String, Object> entry) {
    final String normalizedName = normalizeName(entry.getKey());
    requireArgument(
        !normalizedName.isBlank(),
        "Column name cannot be normalized: " + entry.getKey()
    );
    requireArgument(
        normalizedName.length() <= 120,
        "Normalized column name exceeds 120 characters: " + normalizedName
    );
    return new NormalizedAttribute(
        entry.getKey(),
        normalizedName,
        normalizeValue(entry.getValue()),
        labelAliases.contains(normalizedName)
    );
  }

  private void validateUniqueNames(final List<NormalizedAttribute> attributes) {
    attributes.stream()
        .collect(Collectors.groupingBy(NormalizedAttribute::normalizedName, Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .findFirst()
        .ifPresent(name -> {
          throw new IllegalArgumentException("Duplicated normalized column name: " + name);
        });
  }

  private void validateSingleLabel(final List<NormalizedAttribute> attributes) {
    final long labelCount = attributes.stream().filter(NormalizedAttribute::label).count();
    requireArgument(labelCount <= 1, "Dataset row contains multiple fraud label columns");
  }

  private Object normalizeValue(final Object rawValue) {
    return switch (rawValue) {
      case null -> null;
      case Number number -> number;
      case Boolean booleanValue -> booleanValue;
      case LocalDate date -> date;
      case LocalDateTime dateTime -> dateTime;
      default -> normalizeText(rawValue.toString().trim());
    };
  }

  private Object normalizeText(final String value) {
    return Optional.of(value)
        .filter(text -> !text.isBlank())
        .map(text -> VALUE_NORMALIZERS.stream()
            .map(normalizer -> normalizer.apply(text))
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(text))
        .orElse(null);
  }

  private Boolean parseLabel(final Object value) {
    return switch (value) {
      case null -> null;
      case Boolean booleanValue -> booleanValue;
      case Number number -> parseNumericLabel(number);
      default -> Optional.ofNullable(LABEL_VALUES.get(value.toString().toLowerCase(Locale.ROOT)))
          .orElseThrow(() -> new IllegalArgumentException(
              "Invalid confirmed fraud label: " + value
          ));
    };
  }

  private Boolean parseNumericLabel(final Number value) {
    final BigDecimal numericLabel;
    try {
      numericLabel = new BigDecimal(value.toString()).stripTrailingZeros();
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("Invalid numeric fraud label: " + value, exception);
    }
    return Optional.of(numericLabel)
        .filter(label -> label.compareTo(BigDecimal.ZERO) == 0
            || label.compareTo(BigDecimal.ONE) == 0)
        .map(label -> label.compareTo(BigDecimal.ONE) == 0)
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid numeric fraud label: " + value
        ));
  }

  private String typeOf(final Object value) {
    return switch (value) {
      case null -> "NULL";
      case Boolean ignored -> "BOOLEAN";
      case Number ignored -> "NUMBER";
      case LocalDate ignored -> "DATE";
      case LocalDateTime ignored -> "DATE";
      default -> "STRING";
    };
  }

  private static Optional<Object> parseBooleanValue(final String value) {
    return Optional.ofNullable(Map.of(
        "true", true,
        "sim", true,
        "yes", true,
        "false", false,
        "nao", false,
        "não", false,
        "no", false
    ).get(value.toLowerCase(Locale.ROOT)));
  }

  private static Optional<Object> parseDateValue(final String value) {
    return DATE_FORMATS.stream()
        .map(format -> tryParseDate(value, format))
        .flatMap(Optional::stream)
        .map(Object.class::cast)
        .findFirst();
  }

  private static Optional<LocalDate> tryParseDate(
      final String value,
      final DateTimeFormatter formatter) {
    try {
      return Optional.of(LocalDate.parse(value, formatter));
    } catch (DateTimeParseException ignored) {
      return Optional.empty();
    }
  }

  private static Optional<Object> parseBrazilianMoney(final String value) {
    return Optional.of(value)
        .filter(text -> BRAZILIAN_MONEY.matcher(text).matches())
        .map(text -> new BigDecimal(
            text.replace("R$", "").replace(" ", "").replace(".", "").replace(',', '.')
        ));
  }

  private static Optional<Object> parseInteger(final String value) {
    return Optional.of(value)
        .filter(text -> INTEGER.matcher(text).matches())
        .flatMap(DynamicDatasetProcessor::tryParseLong);
  }

  private static Optional<Object> tryParseLong(final String value) {
    try {
      return Optional.of(Long.parseLong(value));
    } catch (NumberFormatException ignored) {
      return Optional.empty();
    }
  }

  private static Optional<Object> parseDecimal(final String value) {
    return Optional.of(value)
        .filter(text -> DECIMAL.matcher(text).matches())
        .flatMap(DynamicDatasetProcessor::tryParseDecimal);
  }

  private static Optional<Object> tryParseDecimal(final String value) {
    try {
      return Optional.of(new BigDecimal(value.replace(',', '.')));
    } catch (NumberFormatException ignored) {
      return Optional.empty();
    }
  }

  private record NormalizedAttribute(
      String originalName,
      String normalizedName,
      Object value,
      boolean label) {
  }
}
