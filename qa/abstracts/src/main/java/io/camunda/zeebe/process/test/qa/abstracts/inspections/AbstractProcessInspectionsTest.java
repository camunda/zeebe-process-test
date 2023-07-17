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

package io.camunda.zeebe.process.test.qa.abstracts.inspections;

import static io.camunda.zeebe.process.test.inspections.ProcessDefinitionInspectionUtility.getBpmnElementId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackNamedElements;
import org.junit.jupiter.api.Test;

public abstract class AbstractProcessInspectionsTest {
  @Test
  void testFindStartEventIdByName() {
    // given
    Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME);
    Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME_V2);
    // when
    String startEventId = getBpmnElementId(ProcessPackNamedElements.START_EVENT_NAME);
    // then
    assertThat(startEventId).isEqualTo(ProcessPackNamedElements.START_EVENT_ID);
  }

  @Test
  void testFindEndEventIdByBpmnProcessIdAndName() {
    // given
    Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME);
    Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME_V2);
    // when
    String endEventId =
        getBpmnElementId(
            ProcessPackNamedElements.PROCESS_ID, ProcessPackNamedElements.END_EVENT_NAME);
    // then
    assertThat(endEventId).isEqualTo(ProcessPackNamedElements.END_EVENT_ID);
  }

  @Test
  void testFindEndEventIdByDeploymentAndName() {
    // given
    DeploymentEvent deployment =
        Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME);
    // when
    String endEventId = getBpmnElementId(deployment, ProcessPackNamedElements.END_EVENT_NAME);
    // then
    assertThat(endEventId).isEqualTo(ProcessPackNamedElements.END_EVENT_ID);
  }

  @Test
  void testThrowIfNameIsNotUnique() {
    // given
    Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME);
    Utilities.deployResource(getClient(), ProcessPackNamedElements.RESOURCE_NAME_V2);
    // when, then
    assertThatThrownBy(() -> getBpmnElementId(ProcessPackNamedElements.TASK_NAME))
        .isInstanceOf(AssertionError.class);
  }

  public abstract ZeebeClient getClient();

  public abstract ZeebeTestEngine getEngine();
}
