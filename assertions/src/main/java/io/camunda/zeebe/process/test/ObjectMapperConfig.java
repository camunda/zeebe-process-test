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

package io.camunda.zeebe.process.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.impl.ZeebeObjectMapper;

/** Shared custom {@link ObjectMapper} configured by the user */
public class ObjectMapperConfig {

  private static final ThreadLocal<ZeebeObjectMapper> objectMapper = new ThreadLocal<>();

  static {
    objectMapper.set(new ZeebeObjectMapper());
  }

  /**
   * @return the configured {@link ObjectMapper}.
   */
  public static ZeebeObjectMapper getObjectMapper() {
    return ObjectMapperConfig.objectMapper.get();
  }

  public static void initObjectMapper(final ObjectMapper objectMapper) {
    ObjectMapperConfig.objectMapper.set(new ZeebeObjectMapper(objectMapper));
  }
}
