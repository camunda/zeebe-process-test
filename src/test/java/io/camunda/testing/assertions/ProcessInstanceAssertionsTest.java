package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.BpmnAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// TODO remove Thread.sleeps
@ZeebeAssertions
class ProcessInstanceAssertionsTest {

  public static final String PROCESS_INSTANCE_BPMN = "looping-servicetask.bpmn";
  public static final String PROCESS_INSTANCE_ID = "looping-servicetask";
  public static final String MULTIPLE_TASKS_BPMN = "multiple-tasks.bpmn";
  public static final String MULTIPLE_TASKS_PROCESS_ID = "multiple-tasks";
  public static final String MESSAGE_EVENT_BPMN = "message-event.bpmn";
  public static final String MESSAGE_EVENT_PROCESS_ID = "message-event";
  public static final String STARTEVENT = "startevent";
  public static final String SERVICETASK = "servicetask";
  public static final String SERVICETASK_1 = "servicetask1";
  public static final String SERVICETASK_2 = "servicetask2";
  public static final String SERVICETASK_3 = "servicetask3";
  public static final String ENDEVENT = "endevent";
  public static final String VAR_TOTAL_LOOPS = "totalLoops";
  public static final String MESSAGE_NAME = "message";

  private ZeebeClient client;
  private ZeebeEngine engine;

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testProcessInstanceIsStarted() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).isStarted();
    }

    @Test
    public void testProcessInstanceIsActive() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).isActive();
    }

    @Test
    public void testProcessInstanceIsCompleted() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(SERVICETASK);

      // then
      assertThat(instanceEvent).isCompleted();
    }

    @Test
    public void testProcessInstanceIsNotCompleted() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).isNotCompleted();
    }

    @Test
    public void testProcessInstanceIsTerminated() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Thread.sleep(100);

      // then
      assertThat(instanceEvent).isTerminated();
    }

    @Test
    public void testProcessInstanceIsNotTerminated() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).isNotTerminated();
    }

    @Test
    public void testProcessInstanceHasPassedElement() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(SERVICETASK);

      // then
      assertThat(instanceEvent).hasPassedElement(SERVICETASK);
    }

    @Test
    public void testProcessInstanceHasNotPassedElement() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).hasNotPassedElement(SERVICETASK);
    }

    @Test
    public void testProcessInstanceHasPassedElementMultipleTimes() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final int totalLoops = 5;
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, totalLoops);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      for (int i = 0; i < 5; i++) {
        completeTask(SERVICETASK);
      }

      // then
      assertThat(instanceEvent).hasPassedElement(SERVICETASK, totalLoops);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrder() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(SERVICETASK);

      // then
      assertThat(instanceEvent).hasPassedElementInOrder(STARTEVENT, SERVICETASK, ENDEVENT);
    }

    @Test
    public void testProcessInstanceIsWaitingAt() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThat(instanceEvent).isWaitingAtElement(SERVICETASK_1);
    }

    @Test
    public void testProcessIsWaitingAtMultipleElements() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThat(instanceEvent).isWaitingAtElement(SERVICETASK_1, SERVICETASK_2, SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAt() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask(SERVICETASK_1);

      // then
      assertThat(instanceEvent).isNotWaitingAtElement(SERVICETASK_1);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElements() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask(SERVICETASK_1);
      completeTask(SERVICETASK_2);
      completeTask(SERVICETASK_3);

      // then
      assertThat(instanceEvent).isNotWaitingAtElement(SERVICETASK_1, SERVICETASK_2, SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtNonExistingElement() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);
      final String nonExistingElementId = "non-existing-task";

      // when
      completeTask(nonExistingElementId);

      // then
      assertThat(instanceEvent).isNotWaitingAtElement(nonExistingElementId);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElements() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask(SERVICETASK_1);

      // then
      assertThat(instanceEvent).isWaitingExactlyAtElements(SERVICETASK_2, SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceIsWaitingForMessage() throws InterruptedException {
      // given
      deployProcess(MESSAGE_EVENT_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("correlationKey", "key");

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(MESSAGE_EVENT_PROCESS_ID, variables);

      // then
      assertThat(instanceEvent).isWaitingForMessage(MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceIsNotWaitingForMessage() throws InterruptedException {
      // given
      deployProcess(MESSAGE_EVENT_BPMN);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(MESSAGE_EVENT_PROCESS_ID, variables);

      // when
      sendMessage(MESSAGE_NAME, correlationKey);

      // then
      assertThat(instanceEvent).isNotWaitingForMessage(MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceHasVariable() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).hasVariable(VAR_TOTAL_LOOPS);
    }

    @Test
    public void testProcessInstanceHasVariableWithValue() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, "1");

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).hasVariableWithValue(VAR_TOTAL_LOOPS, "1");
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testProcessInstanceIsStartedFailure() {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
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
      deployProcess(PROCESS_INSTANCE_BPMN);
      startProcessInstance(PROCESS_INSTANCE_ID);
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
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // when
      completeTask(SERVICETASK);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isActive())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s is not active", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsCompletedFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsNotCompletedFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // when
      completeTask(SERVICETASK);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsTerminatedFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceIsNotTerminatedFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Thread.sleep(100);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key %s was terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceHasPassedElementFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasPassedElement(SERVICETASK))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 1 times", SERVICETASK);
    }

    @Test
    public void testProcessInstanceHasNotPassedElementFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap(VAR_TOTAL_LOOPS, 1));

      // when
      completeTask(SERVICETASK);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasNotPassedElement(SERVICETASK))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 0 times", SERVICETASK);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrderFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap(VAR_TOTAL_LOOPS, 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(SERVICETASK);

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .hasPassedElementInOrder(ENDEVENT, SERVICETASK, STARTEVENT))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "[Ordered elements] \n"
                  + "expected: [\"endevent\", \"servicetask\", \"startevent\"]\n"
                  + " but was: [\"startevent\", \"servicetask\", \"endevent\"]");
    }

    @Test
    public void testProcessInstanceIsWaitingAtFailure() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask(SERVICETASK_1);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAtElement(SERVICETASK_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", SERVICETASK_1);
    }

    @Test
    public void testProcessInstanceIsWaitingAtMultipleElementsFailure()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask(SERVICETASK_1);
      completeTask(SERVICETASK_2);
      completeTask(SERVICETASK_3);

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .isWaitingAtElement(SERVICETASK_1, SERVICETASK_2, SERVICETASK_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain:", SERVICETASK_1, SERVICETASK_2, SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceWaitingAtNonExistingElementFailure()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);
      final String nonExistingTaskId = "non-existing-task";

      // when
      completeTask(nonExistingTaskId);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAtElement(nonExistingTaskId))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain", nonExistingTaskId);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtFailure() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotWaitingAtElement(SERVICETASK_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", SERVICETASK_1);
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElementsFailure()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .isNotWaitingAtElement(SERVICETASK_1, SERVICETASK_2, SERVICETASK_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", SERVICETASK_1, SERVICETASK_2, SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooManyElements()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingExactlyAtElements(SERVICETASK_1))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              SERVICETASK_2,
              SERVICETASK_3)
          .hasMessageNotContaining(SERVICETASK_1);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooLittleElements()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);
      completeTask(SERVICETASK_1);
      completeTask(SERVICETASK_2);

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .isWaitingExactlyAtElements(SERVICETASK_1, SERVICETASK_2, SERVICETASK_3))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              SERVICETASK_1,
              SERVICETASK_2)
          .hasMessageNotContaining(SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_combination()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);
      completeTask(SERVICETASK_1);
      completeTask(SERVICETASK_2);

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .isWaitingExactlyAtElements(SERVICETASK_1, SERVICETASK_2))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              SERVICETASK_1,
              SERVICETASK_2,
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              SERVICETASK_3);
    }

    @Test
    public void testProcessInstanceIsWaitingForMessageFailure() throws InterruptedException {
      // given
      deployProcess(MESSAGE_EVENT_BPMN);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(MESSAGE_EVENT_PROCESS_ID, variables);

      // when
      sendMessage(MESSAGE_NAME, correlationKey);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingForMessage(MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("to contain:", MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceIsNotWaitingForMessageFailure() throws InterruptedException {
      // given
      deployProcess(MESSAGE_EVENT_BPMN);
      final String correlationKey = "key";
      final Map<String, Object> variables =
          Collections.singletonMap("correlationKey", correlationKey);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(MESSAGE_EVENT_PROCESS_ID, variables);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotWaitingForMessage(MESSAGE_NAME))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll("not to contain", MESSAGE_NAME);
    }

    @Test
    public void testProcessInstanceHasVariableFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final String expectedVariable = "variable";
      final String actualVariable = "loopAmount";

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(PROCESS_INSTANCE_ID);

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
      deployProcess(PROCESS_INSTANCE_BPMN);
      final String variable = "variable";
      final String expectedValue = "expectedValue";
      final String actualValue = "actualValue";
      final Map<String, Object> variables = Collections.singletonMap(variable, actualValue);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

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
  }

  private void deployProcess(final String process) {
    client.newDeployCommand().addResourceFromClasspath(process).send().join();
  }

  private ProcessInstanceEvent startProcessInstance(final String processId)
      throws InterruptedException {
    return startProcessInstance(processId, new HashMap<>());
  }

  private ProcessInstanceEvent startProcessInstance(
      final String processId, final Map<String, Object> variables) throws InterruptedException {
    final ProcessInstanceEvent instanceEvent =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .variables(variables)
            .send()
            .join();
    Thread.sleep(100);
    return instanceEvent;
  }

  // TODO we need a proper way to complete jobs instead of this hack
  private void completeTask(final String elementId) throws InterruptedException {
    Thread.sleep(100);
    Record<JobRecordValue> lastRecord = null;
    for (Record<JobRecordValue> record : engine.jobRecords().withElementId(elementId)) {
      if (record.getIntent().equals(JobIntent.CREATED)) {
        lastRecord = record;
      }
    }
    if (lastRecord != null) {
      client.newCompleteCommand(lastRecord.getKey()).send().join();
    }
    Thread.sleep(100);
  }

  private void sendMessage(final String messsageName, final String correlationKey)
      throws InterruptedException {
    client
        .newPublishMessageCommand()
        .messageName(messsageName)
        .correlationKey(correlationKey)
        .send()
        .join();
    Thread.sleep(100);
  }
}
