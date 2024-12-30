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
package io.camunda.zeebe.spring.client.jobhandling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
    classes = {ZeebeClientDisabledInZeebeSpringTest.class},
    properties = {"zeebe.client.enabled=false"})
@ZeebeSpringTest
public class ZeebeClientDisabledInZeebeSpringTest {

  @Autowired private ApplicationContext ctx;

  @Test
  public void testStartup() {
    // a testcase with @ZeebeSpringTests ALWAYS creates a ZeebeEngine and a ZeebeClient (!), even
    // when "zeebe.client.enabled=false" is configured
    // In essence, this is an invalid configuration state - when you don't want to use ZeebeClient,
    // don't use @ZeebeSpringTest
    assertEquals(1, ctx.getBeanNamesForType(ZeebeClient.class).length);
    assertEquals(1, ctx.getBeanNamesForType(ZeebeTestEngine.class).length);
  }
}
