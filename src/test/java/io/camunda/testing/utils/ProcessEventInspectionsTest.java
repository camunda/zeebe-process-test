package io.camunda.testing.utils;

import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.increaseTime;
import static io.camunda.testing.utils.InspectionUtility.findProcessEvents;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackTimerStartEvent;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import java.time.Duration;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.camunda.community.eze.ZeebeEngineClock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class ProcessEventInspectionsTest {

  private static final String WRONG_TIMER_ID = "wrongtimer";

  private ZeebeClient client;
  private ZeebeEngine engine;
  private ZeebeEngineClock clock;

  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testAssertThatFirstProcessInstance() throws InterruptedException {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

      // when
      increaseTime(clock, Duration.ofDays(1));

      // then
      findProcessEvents()
          .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
          .withProcessDefinitionKey(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey())
          .assertThatFirstProcessInstance()
          .isCompleted();
    }

    @Test
    public void testAssertThatLastProcessInstance() throws InterruptedException {
      // given
      final DeploymentEvent deploymentEvent =
          deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

      // when
      increaseTime(clock, Duration.ofDays(1));

      // then
      findProcessEvents()
          .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
          .withProcessDefinitionKey(deploymentEvent.getProcesses().get(0).getProcessDefinitionKey())
          .assertThatLastProcessInstance()
          .isCompleted();
    }
  }

  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testAssertThatFirstProcessInstance() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackTimerStartEvent.RESOURCE_NAME);

      // when
      increaseTime(clock, Duration.ofDays(1));

      // then
      assertThatThrownBy(
              () ->
                  findProcessEvents()
                      .triggeredByTimer(WRONG_TIMER_ID)
                      .assertThatFirstProcessInstance()
                      .isCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No process instances have been found");
    }
  }
}
