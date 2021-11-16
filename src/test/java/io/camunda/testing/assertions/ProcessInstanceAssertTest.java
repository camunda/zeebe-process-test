package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.util.Utilities.completeTask;
import static io.camunda.testing.util.Utilities.deployProcess;
import static io.camunda.testing.util.Utilities.sendMessage;
import static io.camunda.testing.util.Utilities.startProcessInstance;
import static io.camunda.testing.util.Utilities.waitForIdleState;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackLoopingServiceTask;
import io.camunda.testing.util.Utilities.ProcessPackMessageEvent;
import io.camunda.testing.util.Utilities.ProcessPackMultipleTasks;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import java.util.Collections;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
class ProcessInstanceAssertTest {

  private ZeebeEngine engine;

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;

    @Test
    public void testProcessInstanceIsStarted() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).isStarted();
    }

    @Test
    public void testProcessInstanceIsActive() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).isActive();
    }

    @Test
    public void testProcessInstanceIsCompleted() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThat(instanceEvent).isCompleted();
    }

    @Test
    public void testProcessInstanceIsNotCompleted() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).isNotCompleted();
    }

    @Test
    public void testProcessInstanceIsTerminated() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      waitForIdleState(engine);

      // then
      assertThat(instanceEvent).isTerminated();
    }

    @Test
    public void testProcessInstanceIsNotTerminated() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).isNotTerminated();
    }

    @Test
    public void testProcessInstanceHasPassedElement() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThat(instanceEvent).hasPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedElement() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).hasNotPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedElementMultipleTimes() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final int totalLoops = 5;
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, totalLoops);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      for (int i = 0; i < 5; i++) {
        completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);
      }

      // then
      assertThat(instanceEvent).hasPassedElement(ProcessPackLoopingServiceTask.ELEMENT_ID,
          totalLoops);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrder() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThat(instanceEvent).hasPassedElementInOrder(
          ProcessPackLoopingServiceTask.START_EVENT_ID, ProcessPackLoopingServiceTask.ELEMENT_ID,
          ProcessPackLoopingServiceTask.END_EVENT_ID);
    }

    @Test
    public void testProcessInstanceIsWaitingAt() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThat(instanceEvent).isWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessIsWaitingAtMultipleElements() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThat(instanceEvent).isWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1,
          ProcessPackMultipleTasks.ELEMENT_ID_2, ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAt() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      assertThat(instanceEvent).isNotWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElements() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_3);

      // then
      assertThat(instanceEvent).isNotWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1,
          ProcessPackMultipleTasks.ELEMENT_ID_2, ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtNonExistingElement() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);
      final String nonExistingElementId = "non-existing-task";

      // when
      completeTask(engine, client, nonExistingElementId);

      // then
      assertThat(instanceEvent).isNotWaitingAtElement(nonExistingElementId);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElements() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      assertThat(instanceEvent).isWaitingExactlyAtElements(ProcessPackMultipleTasks.ELEMENT_ID_2,
          ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingForMessage() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE, "key");

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).isWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceIsNotWaitingForMessage() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE,
              correlationKey);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      assertThat(instanceEvent).isNotWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceHasVariable() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).hasVariable(ProcessPackLoopingServiceTask.TOTAL_LOOPS);
    }

    @Test
    public void testProcessInstanceHasVariableWithValue() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, "1");

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThat(instanceEvent)
          .hasVariableWithValue(ProcessPackLoopingServiceTask.TOTAL_LOOPS, "1");
    }

    @Test
    public void testHasCorrelatedMessageByName() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      assertThat(instanceEvent).hasCorrelatedMessageByName(ProcessPackMessageEvent.MESSAGE_NAME, 1);
    }

    @Test
    public void testHasCorrelatedMessageByCorrelationKey() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackMessageEvent.CORRELATION_KEY_VARIABLE,
              correlationKey);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      assertThat(instanceEvent).hasCorrelatedMessageByCorrelationKey(correlationKey, 1);
    }

    @Test
    public void testHasAnyIncidents() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS,
              "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because ProcessPackLoopingServiceTask.TOTAL_LOOPS is a string, but needs to be an int */
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThat(instanceEvent).hasAnyIncidents();
    }

    @Test
    public void testHasNoIncidents() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThat(instanceEvent).hasNoIncidents();
    }

    @Test
    public void testExtractLatestIncident() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS,
              "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because ProcessPackLoopingServiceTask.TOTAL_LOOPS is a string, but needs to be an int */
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      final IncidentAssert incidentAssert = assertThat(instanceEvent).extractLatestIncident();

      // then

      Assertions.assertThat(incidentAssert).isNotNull();
      incidentAssert
          .isUnresolved()
          .hasErrorType(ErrorType.EXTRACT_VALUE_ERROR)
          .wasRaisedInProcessInstance(instanceEvent);
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;
    private ZeebeClient client;

    @Test
    public void testProcessInstanceIsStartedFailure() {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      assertThrows(
          AssertionError.class,
          assertThat(mockInstanceEvent)::isStarted,
          "Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceIsNotStartedIfProcessInstanceKeyNoMatch()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      assertThatThrownBy(() -> assertThat(mockInstanceEvent).isStarted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceIsActiveFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(
              client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isActive())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s is not active", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsCompletedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsNotCompletedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsTerminatedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsNotTerminatedFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      waitForIdleState(engine);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceHasPassedElementFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasPassedElement(
          ProcessPackLoopingServiceTask.ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 1 times",
              ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedElementFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID,
              Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1));

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasNotPassedElement(
          ProcessPackLoopingServiceTask.ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 0 times",
              ProcessPackLoopingServiceTask.ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrderFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables = Collections.singletonMap(
          ProcessPackLoopingServiceTask.TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // when
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(
          () ->
              assertThat(instanceEvent)
                  .hasPassedElementInOrder(
                      ProcessPackLoopingServiceTask.END_EVENT_ID,
                      ProcessPackLoopingServiceTask.ELEMENT_ID,
                      ProcessPackLoopingServiceTask.START_EVENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "[Ordered elements] \n"
                  + "expected: [\"endevent\", \"servicetask\", \"startevent\"]\n"
                  + " but was: [\"startevent\", \"servicetask\", \"endevent\"]");
    }

    @Test
    public void testProcessInstanceIsWaitingAtFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);

      // then
      assertThatThrownBy(
          () -> assertThat(instanceEvent).isWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsWaitingAtMultipleElementsFailure()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // when
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_3);

      // then
      assertThatThrownBy(
          () ->
              assertThat(instanceEvent)
                  .isWaitingAtElement(
                      ProcessPackMultipleTasks.ELEMENT_ID_1,
                      ProcessPackMultipleTasks.ELEMENT_ID_2,
                      ProcessPackMultipleTasks.ELEMENT_ID_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain:", ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2, ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceWaitingAtNonExistingElementFailure()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);
      final String nonExistingTaskId = "non-existing-task";

      // when
      completeTask(engine, client, nonExistingTaskId);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAtElement(nonExistingTaskId))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", nonExistingTaskId);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent)
          .isNotWaitingAtElement(ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElementsFailure()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(
          () ->
              assertThat(instanceEvent)
                  .isNotWaitingAtElement(
                      ProcessPackMultipleTasks.ELEMENT_ID_1,
                      ProcessPackMultipleTasks.ELEMENT_ID_2,
                      ProcessPackMultipleTasks.ELEMENT_ID_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2, ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooManyElements()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingExactlyAtElements(
          ProcessPackMultipleTasks.ELEMENT_ID_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              ProcessPackMultipleTasks.ELEMENT_ID_3)
          .hasMessageNotContaining(ProcessPackMultipleTasks.ELEMENT_ID_1);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooLittleElements()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);

      // then
      assertThatThrownBy(
          () ->
              assertThat(instanceEvent)
                  .isWaitingExactlyAtElements(
                      ProcessPackMultipleTasks.ELEMENT_ID_1,
                      ProcessPackMultipleTasks.ELEMENT_ID_2,
                      ProcessPackMultipleTasks.ELEMENT_ID_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2)
          .hasMessageNotContaining(ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_combination()
        throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMultipleTasks.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMultipleTasks.PROCESS_ID);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_1);
      completeTask(engine, client, ProcessPackMultipleTasks.ELEMENT_ID_2);

      // then
      assertThatThrownBy(
          () ->
              assertThat(instanceEvent)
                  .isWaitingExactlyAtElements(
                      ProcessPackMultipleTasks.ELEMENT_ID_1,
                      ProcessPackMultipleTasks.ELEMENT_ID_2))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_1,
              ProcessPackMultipleTasks.ELEMENT_ID_2,
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              ProcessPackMultipleTasks.ELEMENT_ID_3);
    }

    @Test
    public void testProcessInstanceIsWaitingForMessageFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // when
      sendMessage(client, ProcessPackMessageEvent.MESSAGE_NAME, correlationKey);

      // then
      assertThatThrownBy(
          () -> assertThat(instanceEvent).isWaitingForMessage(ProcessPackMessageEvent.MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain:", ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceIsNotWaitingForMessageFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotWaitingForMessage(
          ProcessPackMessageEvent.MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", ProcessPackMessageEvent.MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceHasVariableFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final String expectedVariable = "variable";
      final String actualVariable = "loopAmount";

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(client,
          ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasVariable(expectedVariable))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s does not contain variable with name `%s`. Available variables are: [%s]",
              instanceEvent.getProcessInstanceKey(), expectedVariable, actualVariable);
    }

    @Test
    public void testProcessInstanceHasVariableWithValueFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final String variable = "variable";
      final String expectedValue = "expectedValue";
      final String actualValue = "actualValue";
      final Map<String, Object> variables = Collections.singletonMap(variable, actualValue);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
          () -> assertThat(instanceEvent).hasVariableWithValue(variable, expectedValue))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "The variable '%s' does not have the expected value. The value passed in"
                  + " ('%s') is internally mapped to a JSON String that yields '\"%s\"'. However, the "
                  + "actual value (as JSON String) is '\"%s\".",
              variable, expectedValue, expectedValue, actualValue);
    }

    @Test
    public void testHasCorrelatedMessageByNameFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
          () -> assertThat(instanceEvent).hasCorrelatedMessageByName(
              ProcessPackMessageEvent.MESSAGE_NAME, 1))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected message with name '%s' to be correlated %d times, but was %d times",
              ProcessPackMessageEvent.MESSAGE_NAME, 1, 0);
    }

    @Test
    public void testHasCorrelatedMessageByCorrelationKeyFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackMessageEvent.RESOURCE_NAME);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackMessageEvent.PROCESS_ID, variables);

      // then
      assertThatThrownBy(
          () -> assertThat(instanceEvent).hasCorrelatedMessageByCorrelationKey(correlationKey, 1))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Expected message with correlation key '%s' to be correlated %d "
                  + "times, but was %d times",
              correlationKey, 1, 0);
    }

    @Test
    public void testHasAnyIncidentsFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasAnyIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this process instance");
    }

    @Test
    public void testHasNoIncidentsFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);
      final Map<String, Object> variables =
          Collections.singletonMap(ProcessPackLoopingServiceTask.TOTAL_LOOPS,
              "invalid value"); // will cause incident

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID, variables);
      /* will raise an incident in the gateway because ProcessPackLoopingServiceTask.TOTAL_LOOPS is a string, but needs to be an int */
      completeTask(engine, client, ProcessPackLoopingServiceTask.ELEMENT_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasNoIncidents())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Incidents were raised for this process instance");
    }

    @Test
    public void testExtractLatestIncidentFailure() throws InterruptedException {
      // given
      deployProcess(client, ProcessPackLoopingServiceTask.RESOURCE_NAME);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(client, ProcessPackLoopingServiceTask.PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).extractLatestIncident())
          .isInstanceOf(AssertionError.class)
          .hasMessage("No incidents were raised for this process instance");
    }
  }
}
