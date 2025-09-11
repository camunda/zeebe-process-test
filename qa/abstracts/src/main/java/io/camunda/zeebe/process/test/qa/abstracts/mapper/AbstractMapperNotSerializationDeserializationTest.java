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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.JsonMapper;
import io.camunda.zeebe.client.api.command.InternalClientException;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractMapperNotSerializationDeserializationTest {
  protected ZeebeClient client;
  protected ZeebeTestEngine engine;

  @Test
  void shouldNotSerialize() {
    final JsonMapper jsonMapper = client.getConfiguration().getJsonMapper();
    final LocalDateTime localDateTime = LocalDateTime.of(2023, 7, 31, 12, 0, 0);
    Assertions.assertThrows(InternalClientException.class, () -> jsonMapper.toJson(localDateTime));
  }

  @Test
  void shouldNotDeserialize() {
    final JsonMapper jsonMapper = client.getConfiguration().getJsonMapper();
    final String json = "\"2023-07-31T12:00:00\"";
    Assertions.assertThrows(
        InternalClientException.class, () -> jsonMapper.fromJson(json, LocalDateTime.class));
  }
}
