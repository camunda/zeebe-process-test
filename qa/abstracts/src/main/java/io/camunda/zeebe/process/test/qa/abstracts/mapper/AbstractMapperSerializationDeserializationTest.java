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

package io.camunda.zeebe.process.test.qa.abstracts.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.JsonMapper;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public abstract class AbstractMapperSerializationDeserializationTest {
  protected CamundaClient client;
  protected ZeebeTestEngine engine;
  protected ObjectMapper mapper = configureObjectMapper();

  @Test
  void shouldSerialize() {
    final JsonMapper jsonMapper = client.getConfiguration().getJsonMapper();
    final LocalDateTime localDateTime = LocalDateTime.of(2023, 7, 31, 12, 0, 0);
    final String expectedJson = "\"2023-07-31T12:00:00\"";
    final String actualJson = jsonMapper.toJson(localDateTime);
    assertEquals(expectedJson, actualJson);
  }

  @Test
  void shouldDeserialize() {
    final JsonMapper jsonMapper = client.getConfiguration().getJsonMapper();
    final String json = "\"2023-07-31T12:00:00\"";
    final LocalDateTime expectedLocalDateTime = LocalDateTime.of(2023, 7, 31, 12, 0, 0);
    final LocalDateTime actualLocalDateTime = jsonMapper.fromJson(json, LocalDateTime.class);
    assertEquals(expectedLocalDateTime, actualLocalDateTime);
  }

  private ObjectMapper configureObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return objectMapper;
  }
}
