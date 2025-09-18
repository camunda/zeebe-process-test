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

package io.camunda.zeebe.process.test.qa.embedded.inspections;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.qa.abstracts.inspections.AbstractProcessInspectionsTest;

@ZeebeProcessTest
public class ProcessInspectionsTest extends AbstractProcessInspectionsTest {
  private ZeebeClient client;
  private ZeebeTestEngine engine;

  @Override
  public ZeebeClient getClient() {
    return client;
  }

  @Override
  public ZeebeTestEngine getEngine() {
    return engine;
  }
}
