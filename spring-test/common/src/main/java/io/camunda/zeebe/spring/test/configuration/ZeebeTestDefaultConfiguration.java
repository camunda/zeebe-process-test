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
package io.camunda.zeebe.spring.test.configuration;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.api.JsonMapper;
import io.camunda.client.impl.CamundaObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** Fallback values if certain beans are missing */
public class ZeebeTestDefaultConfiguration {

  @Bean(name = "camundaJsonMapper")
  @ConditionalOnMissingBean
  public JsonMapper jsonMapper(final ObjectMapper objectMapper) {
    return new CamundaObjectMapper(objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
  }

  @Bean
  @ConditionalOnMissingBean
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}
