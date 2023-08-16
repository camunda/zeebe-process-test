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
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackLoopingServiceTask;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

public abstract class AbstractCustomObjectMapperTest {

  private ZeebeClient client;
  private ZeebeTestEngine engine;

  private final ObjectMapper objectMapper = configureObjectMapper();

  @Test
  public void shouldDeserializeDateVariables() throws InterruptedException, TimeoutException {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("stringProperty", "stringValue");
    variables.put("numberProperty", 123);
    variables.put("booleanProperty", true);
    variables.put("complexProperty", Arrays.asList("Element 1", "Element 2"));
    variables.put("nullProperty", null);
    variables.put("javaDate", LocalDateTime.of(2023, 8, 14, 16, 0, 0));
    variables.put("stringDate", "\"2023-08-14T16:00:00\"");
    // given
    Utilities.deployResource(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

    // when
    final ProcessInstanceEvent instanceEvent =
        Utilities.startProcessInstance(
            engine, client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

    // then
    assertThatNoException()
        .isThrownBy(
            () ->
                variables.forEach(
                    (key, value) ->
                        BpmnAssert.assertThat(instanceEvent).hasVariableWithValue(key, value)));
  }

  private ObjectMapper configureObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return objectMapper;
  }
}
