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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.inspections.InspectionUtility;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities;
import io.camunda.zeebe.process.test.qa.abstracts.util.Utilities.ProcessPackTimerStartEvent;
import java.time.Duration;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractProcessEventInspectionsTest {

  private static final String WRONG_TIMER_ID = "wrongtimer";

  @Test
  void testFindFirstProcessInstance() throws InterruptedException {
    // given
    final DeploymentEvent deploymentEvent =
        Utilities.deployResource(getClient(), ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(getEngine(), Duration.ofDays(1));
    final Optional<InspectedProcessInstance> firstProcessInstance =
        InspectionUtility.findProcessEvents()
            .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
            .withProcessDefinitionKey(
                deploymentEvent.getProcesses().get(0).getProcessDefinitionKey())
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isNotEmpty();
    BpmnAssert.assertThat(firstProcessInstance.get()).isCompleted();
  }

  @Test
  void testFindLastProcessInstance() throws InterruptedException {
    // given
    final DeploymentEvent deploymentEvent =
        Utilities.deployResource(getClient(), ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(getEngine(), Duration.ofDays(1));
    final Optional<InspectedProcessInstance> lastProcessInstance =
        InspectionUtility.findProcessEvents()
            .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
            .withProcessDefinitionKey(
                deploymentEvent.getProcesses().get(0).getProcessDefinitionKey())
            .findLastProcessInstance();

    // then
    Assertions.assertThat(lastProcessInstance).isNotEmpty();
    BpmnAssert.assertThat(lastProcessInstance.get()).isCompleted();
  }

  @Test
  void testFindFirstProcessInstance_wrongTimer() throws InterruptedException {
    // given
    Utilities.deployResource(getClient(), ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(getEngine(), Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        InspectionUtility.findProcessEvents()
            .triggeredByTimer(WRONG_TIMER_ID)
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }

  @Test
  void testFindProcessInstance_highIndex() throws InterruptedException {
    // given
    Utilities.deployResource(getClient(), ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(getEngine(), Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        InspectionUtility.findProcessEvents().findProcessInstance(10);

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }

  public abstract ZeebeClient getClient();

  public abstract ZeebeTestEngine getEngine();
}
