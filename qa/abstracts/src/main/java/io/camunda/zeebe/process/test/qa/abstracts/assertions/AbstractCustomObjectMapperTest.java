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

package io.camunda.zeebe.process.test.qa.abstracts.assertions;

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackLoopingServiceTask;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractCustomObjectMapperTest {

  private static Stream<Arguments> provideVariables() {
    return Stream.of(
        Arguments.of("stringProperty", "stringValue"),
        Arguments.of("booleanProperty", true),
        Arguments.of("complexProperty", Arrays.asList("Element 1", "Element 2")),
        Arguments.of("nullProperty", null),
        Arguments.of("javaDate", LocalDateTime.of(2023, 8, 14, 16, 0, 0)),
        Arguments.of("stringDate", "\"2023-08-14T16:00:00\""));
  }

  @Nested
  class HappyPathTests {

    private CamundaClient client;
    private ZeebeTestEngine engine;

    private final ObjectMapper objectMapper = configureObjectMapper();

    @ParameterizedTest
    @MethodSource(
        "io.camunda.zeebe.process.test.qa.abstracts.assertions.AbstractCustomObjectMapperTest#provideVariables")
    public void shouldDeserializeDateVariables(final String key, final Object value)
        throws InterruptedException, TimeoutException {
      // given
      Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          Utilities.startProcessInstance(
              engine,
              client,
              ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(key, value));

      // then
      assertThatNoException()
          .isThrownBy(() -> BpmnAssert.assertThat(instanceEvent).hasVariableWithValue(key, value));
    }

    private ObjectMapper configureObjectMapper() {
      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      return objectMapper;
    }
  }
}
