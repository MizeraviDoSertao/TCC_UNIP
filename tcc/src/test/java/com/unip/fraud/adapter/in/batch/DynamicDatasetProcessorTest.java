package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicDatasetProcessorTest {

  private final DynamicDatasetProcessor processor = new DynamicDatasetProcessor(
      java.util.Set.of("fraudfound_p", "fraud", "is_fraud", "fraude", "isfraud", "label")
  );

  @Test
  void keepsUnknownColumnsAndLeavesConfirmationUnknownWhenLabelIsMissing() {
    final Map<String, Object> values = new LinkedHashMap<>();
    values.put("Valor do Sinistro", "42500,50");
    values.put("Coluna Futura", "preservada");

    final var result = processor.process(row(values));

    assertThat(result.rejected()).isFalse();
    assertThat(result.transaction().realFraud()).isNull();
    assertThat(result.transaction().features())
        .containsEntry("valor_do_sinistro", new java.math.BigDecimal("42500.50"))
        .containsEntry("coluna_futura", "preservada");
  }

  @Test
  void recognizesAnAliasedConfirmedFraudColumnAndExcludesItFromFeatures() {
    final var result = processor.process(row(Map.of(
        "is_fraud", "sim",
        "amount", "100"
    )));

    assertThat(result.rejected()).isFalse();
    assertThat(result.transaction().realFraud()).isTrue();
    assertThat(result.transaction().features())
        .containsEntry("amount", 100L)
        .doesNotContainKey("is_fraud");
  }

  @Test
  void rejectsAnInvalidConfirmedFraudLabelWithoutLosingTheRawRow() {
    final DatasetRow row = row(Map.of("fraude", "talvez", "amount", "100"));

    final var result = processor.process(row);

    assertThat(result.rejected()).isTrue();
    assertThat(result.source()).isEqualTo(row);
    assertThat(result.error()).contains("Invalid confirmed fraud label");
  }

  @Test
  void rejectsNumericFraudLabelsOtherThanZeroOrOne() {
    final DatasetRow row = row(Map.of("fraud", "2", "amount", "100"));

    final var result = processor.process(row);

    assertThat(result.rejected()).isTrue();
    assertThat(result.error()).contains("Invalid numeric fraud label");
  }

  @Test
  void rejectsDifferentHeadersThatNormalizeToTheSameName() {
    final Map<String, Object> values = new LinkedHashMap<>();
    values.put("Claim ID", "A-1");
    values.put("claim-id", "A-2");

    final var result = processor.process(row(values));

    assertThat(result.rejected()).isTrue();
    assertThat(result.error()).contains("Duplicated normalized column name");
  }

  private DatasetRow row(Map<String, Object> values) {
    return new DatasetRow(UUID.randomUUID(), "claims.xlsx", "Sinistros", 2, values);
  }
}
