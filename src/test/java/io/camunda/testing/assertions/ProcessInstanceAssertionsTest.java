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
  public static final String ELEMENT_ID = "servicetask";
  public static final String MULTIPLE_TASKS_BPMN = "multiple-tasks.bpmn";
  public static final String MULTIPLE_TASKS_PROCESS_ID = "multiple-tasks";
  public static final String MESSAGE_EVENT_BPMN = "message-event.bpmn";
  public static final String MESSAGE_EVENT_PROCESS_ID = "message-event";
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
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);

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
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);

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
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThat(instanceEvent).isCompleted();
    }

    @Test
    public void testProcessInstanceIsNotCompleted() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);

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
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
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
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);

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
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThat(instanceEvent).hasPassedElement(ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedElement() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // then
      assertThat(instanceEvent).hasNotPassedElement(ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedElementMultipleTimes() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final int totalLoops = 5;
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", totalLoops);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      for (int i = 0; i < 5; i++) {
        completeTask(ELEMENT_ID);
      }

      // then
      assertThat(instanceEvent).hasPassedElement(ELEMENT_ID, totalLoops);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrder() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThat(instanceEvent).hasPassedElementInOrder("startevent", ELEMENT_ID, "endevent");
    }

    @Test
    public void testProcessInstanceIsWaitingAt() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThat(instanceEvent).isWaitingAtElement("servicetask1");
    }

    @Test
    public void testProcessIsWaitingAtMultipleElements() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThat(instanceEvent).isWaitingAtElement("servicetask1", "servicetask2", "servicetask3");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAt() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("servicetask1");

      // then
      assertThat(instanceEvent).isNotWaitingAtElement("servicetask1");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElements() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("servicetask1");
      completeTask("servicetask2");
      completeTask("servicetask3");

      // then
      assertThat(instanceEvent)
          .isNotWaitingAtElement("servicetask1", "servicetask2", "servicetask3");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtNonExistingElement() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("non-existing-task");

      // then
      assertThat(instanceEvent).isNotWaitingAtElement("non-existing-task");
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElements() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("servicetask1");

      // then
      assertThat(instanceEvent).isWaitingExactlyAtElements("servicetask2", "servicetask3");
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
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

      // when
      completeTask(ELEMENT_ID);

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
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

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
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

      // when
      completeTask(ELEMENT_ID);

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
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

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
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

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
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasPassedElement(ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 1 times", ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedElementFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, Collections.singletonMap("totalLoops", 1));

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasNotPassedElement(ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 0 times", ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedElementsInOrderFailure() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final Map<String, Object> variables = Collections.singletonMap("totalLoops", 1);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_INSTANCE_ID, variables);

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .hasPassedElementInOrder("endevent", ELEMENT_ID, "startevent"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "\nexpected: [\"endevent\", \"servicetask\", \"startevent\"]\n "
                  + "but was: [\"startevent\", \"servicetask\", \"endevent\"]");
    }

    @Test
    public void testProcessInstanceIsWaitingAtFailure() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("servicetask1");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAtElement("servicetask1"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is not waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "servicetask1");
    }

    @Test
    public void testProcessInstanceIsWaitingAtMultipleElementsFailure() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("servicetask1");
      completeTask("servicetask2");
      completeTask("servicetask3");

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .isWaitingAtElement("servicetask1", "servicetask2", "servicetask3"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is not waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "servicetask1, servicetask2, servicetask3");
    }

    @Test
    public void testProcessInstanceWaitingAtNonExistingElementFailure() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // when
      completeTask("non-existing-task");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAtElement("non-existing-task"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is not waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "non-existing-task");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtFailure() throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotWaitingAtElement("servicetask1"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "servicetask1");
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
                      .isNotWaitingAtElement("servicetask1", "servicetask2", "servicetask3"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "servicetask1, servicetask2, servicetask3");
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooManyElements()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingExactlyAtElements("servicetask1"))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              "servicetask2",
              "servicetask3")
          .hasMessageNotContaining("servicetask1");
    }

    @Test
    public void testProcessInstanceIsWaitingExactlyAtElementsFailure_tooLittleElements()
        throws InterruptedException {
      // given
      deployProcess(MULTIPLE_TASKS_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance(MULTIPLE_TASKS_PROCESS_ID);
      completeTask("servicetask1");
      completeTask("servicetask2");

      // then
      assertThatThrownBy(
              () ->
                  assertThat(instanceEvent)
                      .isWaitingExactlyAtElements("servicetask1", "servicetask2", "servicetask3"))
          .isInstanceOf(AssertionError.class)
          .hasMessageContainingAll(
              String.format(
                  "Process with key %s is not waiting at element(s) with id(s)",
                  instanceEvent.getProcessInstanceKey()),
              "servicetask1",
              "servicetask2")
          .hasMessageNotContaining("servicetask3");
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
          .hasMessage(
              "Process with key %s is not waiting for message(s) with name(s) %s",
              instanceEvent.getProcessInstanceKey(), MESSAGE_NAME);
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
          .hasMessage(
              "Process with key %s is waiting for message(s) with name(s) %s",
              instanceEvent.getProcessInstanceKey(), MESSAGE_NAME);
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
