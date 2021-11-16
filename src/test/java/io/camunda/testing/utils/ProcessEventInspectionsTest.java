package io.camunda.testing.utils;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.utils.InspectionUtility.findProcessEvents;
import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.increaseTime;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.utils.model.InspectedProcessInstance;
import io.camunda.testing.util.Utilities.ProcessPackTimerStartEvent;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import java.time.Duration;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.camunda.community.eze.ZeebeEngineClock;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class ProcessEventInspectionsTest {

  private static final String WRONG_TIMER_ID = "wrongtimer";

  private ZeebeClient client;
  private ZeebeEngine engine;
  private ZeebeEngineClock clock;
  private RecordStreamSource recordStreamSource;

  @Test
  public void testFindFirstProcessInstance() throws InterruptedException {
    // given
    final DeploymentEvent deploymentEvent =
        deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(clock, Duration.ofDays(1));
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
  public void testFindLastProcessInstance() throws InterruptedException {
    // given
    final DeploymentEvent deploymentEvent =
        deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(clock, Duration.ofDays(1));
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
  public void testFindFirstProcessInstance_wrongTimer() throws InterruptedException {
    // given
    deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(clock, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        findProcessEvents().triggeredByTimer(WRONG_TIMER_ID).findFirstProcessInstance();

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }

  @Test
  public void testFindProcessInstance_highIndex() throws InterruptedException {
    // given
    deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

    // when
    increaseTime(clock, Duration.ofDays(1));
    final Optional<InspectedProcessInstance> processInstance =
        findProcessEvents().findProcessInstance(10);

    // then
    Assertions.assertThat(processInstance).isEmpty();
  }
}
