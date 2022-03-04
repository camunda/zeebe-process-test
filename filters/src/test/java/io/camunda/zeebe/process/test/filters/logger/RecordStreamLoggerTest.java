/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
