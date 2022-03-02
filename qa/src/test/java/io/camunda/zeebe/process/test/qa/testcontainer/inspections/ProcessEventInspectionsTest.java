package io.camunda.zeebe.process.test.qa.testcontainer.inspections;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;
import io.camunda.zeebe.process.test.inspections.InspectionUtility;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.process.test.qa.util.Utilities;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackTimerStartEvent;
import java.time.Duration;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
class ProcessEventInspectionsTest {

  private static final String WRONG_TIMER_ID = "wrongtimer";

  private ZeebeClient client;
  private InMemoryEngine engine;

  @Test
  void testFindFirstProcessInstance() throws InterruptedException {
    // given
    final DeploymentEvent deploymentEvent =
        Utilities.deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(engine, Duration.ofDays(1));
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
        Utilities.deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(engine, Duration.ofDays(1));
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
    Utilities.deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(engine, Duration.ofDays(1));
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
    Utilities.deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    Utilities.increaseTime(engine, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        InspectionUtility.findProcessEvents().findProcessInstance(10);

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }
}
