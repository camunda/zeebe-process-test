package io.camunda.zeebe.process.test.filters.logger;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RecordStreamLoggerTest {

  @Test
  void testAllValueTypesAreMapped() {
    final Map<ValueType, Function<Record<?>, String>> valueTypeLoggers =
        new RecordStreamLogger(null).getValueTypeLoggers();

    final SoftAssertions softly = new SoftAssertions();
    Arrays.asList(ValueType.values())
        .forEach(
            valueType ->
                softly
                    .assertThat(valueTypeLoggers.containsKey(valueType))
                    .withFailMessage("No value type logger defined for value type '%s'", valueType)
                    .isTrue());

    softly.assertAll();
  }
}
