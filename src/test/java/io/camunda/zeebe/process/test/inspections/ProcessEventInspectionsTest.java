package io.camunda.zeebe.process.test.inspections;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extensions.ZeebeProcessTest;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.process.test.testengine.InMemoryEngine;
import io.camunda.zeebe.process.test.util.Utilities;
import io.camunda.zeebe.process.test.util.Utilities.ProcessPackTimerStartEvent;
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
  public void testFindFirstProcessInstance() {
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
  public void testFindLastProcessInstance() {
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
  public void testFindFirstProcessInstance_wrongTimer() {
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
  public void testFindProcessInstance_highIndex() {
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
