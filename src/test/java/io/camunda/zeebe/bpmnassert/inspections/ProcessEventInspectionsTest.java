package io.camunda.zeebe.bpmnassert.inspections;

import static io.camunda.zeebe.bpmnassert.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.bpmnassert.inspections.InspectionUtility.findProcessEvents;
import static io.camunda.zeebe.bpmnassert.util.Utilities.deployProcess;
import static io.camunda.zeebe.bpmnassert.util.Utilities.increaseTime;

import io.camunda.zeebe.bpmnassert.extensions.ZeebeProcessTest;
import io.camunda.zeebe.bpmnassert.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.bpmnassert.testengine.InMemoryEngine;
import io.camunda.zeebe.bpmnassert.util.Utilities.ProcessPackTimerStartEvent;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
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
        deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(engine, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> firstProcessInstance =
        findProcessEvents()
            .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
            .withProcessDefinitionKey(
                deploymentEvent.getProcesses().get(0).getProcessDefinitionKey())
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isNotEmpty();
    assertThat(firstProcessInstance.get()).isCompleted();
  }

  @Test
  public void testFindLastProcessInstance() {
    // given
    final DeploymentEvent deploymentEvent =
        deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(engine, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> lastProcessInstance =
        findProcessEvents()
            .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
            .withProcessDefinitionKey(
                deploymentEvent.getProcesses().get(0).getProcessDefinitionKey())
            .findLastProcessInstance();

    // then
    Assertions.assertThat(lastProcessInstance).isNotEmpty();
    assertThat(lastProcessInstance.get()).isCompleted();
  }

  @Test
  public void testFindFirstProcessInstance_wrongTimer() {
    // given
    deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(engine, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        findProcessEvents().triggeredByTimer(WRONG_TIMER_ID).findFirstProcessInstance();

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }

  @Test
  public void testFindProcessInstance_highIndex() {
    // given
    deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(engine, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        findProcessEvents().findProcessInstance(10);

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }
}
