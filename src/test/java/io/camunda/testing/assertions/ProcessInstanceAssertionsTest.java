package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.ProcessInstanceAssertions.assertThat;
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

  public static final String PROCESS_INSTANCE_BPMN = "process-instance.bpmn";
  public static final String PROCESS_ID = "processinstance";
  public static final String ELEMENT_ID = "servicetask";

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

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // then
      assertThat(instanceEvent).isStarted();
    }

    @Test
    public void testProcessInstanceIsCompleted() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThat(instanceEvent).isCompleted();
    }

    @Test
    public void testProcessInstanceTerminated() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Thread.sleep(100);

      // then
      assertThat(instanceEvent).isTerminated();
    }

    @Test
    public void testProcessInstanceHasPassed() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThat(instanceEvent).hasPassed(ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassed() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // then
      assertThat(instanceEvent).hasNotPassed(ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedMultipleTimes() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final int totalLoops = 5;
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", totalLoops));

      // when
      for (int i = 0; i < 5; i++) {
        completeTask(ELEMENT_ID);
      }

      // then
      assertThat(instanceEvent).hasPassed(ELEMENT_ID, totalLoops);
    }

    @Test
    public void testProcessInstanceIsWaitingAt() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // then
      assertThat(instanceEvent).isWaitingAt("servicetask1");
    }

    @Test
    public void testProcessIsWaitingAtMultipleElements() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // then
      assertThat(instanceEvent).isWaitingAt("servicetask1", "servicetask2", "servicetask3");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAt() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // when
      completeTask("servicetask1");

      // then
      assertThat(instanceEvent).isNotWaitingAt("servicetask1");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElements() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // when
      completeTask("servicetask1");
      completeTask("servicetask2");
      completeTask("servicetask3");

      // then
      assertThat(instanceEvent).isNotWaitingAt("servicetask1", "servicetask2", "servicetask3");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtNonExistingElement() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // when
      completeTask("non-existing-task");

      // then
      assertThat(instanceEvent).isNotWaitingAt("non-existing-task");
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testProcessInstanceIsNotStarted() {
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
      startProcessInstance(PROCESS_ID);
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      assertThatThrownBy(() -> assertThat(mockInstanceEvent).isStarted())
          .isInstanceOf(AssertionError.class)
          .hasMessage("Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceNotCompleted() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isCompleted())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not completed", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceNotTerminated() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isTerminated())
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s was not terminated", instanceEvent.getProcessInstanceKey());
    }

    @Test
    public void testProcessInstanceHasPassedError() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);

      // when
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasPassed(ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 1 times", ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassedError() throws InterruptedException {
      // given
      deployProcess(PROCESS_INSTANCE_BPMN);
      final ProcessInstanceEvent instanceEvent =
          startProcessInstance(PROCESS_ID, Collections.singletonMap("totalLoops", 1));

      // when
      completeTask(ELEMENT_ID);

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).hasNotPassed(ELEMENT_ID))
          .isInstanceOf(AssertionError.class)
          .hasMessage("Expected element with id %s to be passed 0 times", ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceIsWaitingAtError() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // when
      completeTask("servicetask1");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAt("servicetask1"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is not waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "servicetask1");
    }

    @Test
    public void testProcessInstanceIsWaitingAtMultipleElementsError() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // when
      completeTask("servicetask1");
      completeTask("servicetask2");
      completeTask("servicetask3");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAt("servicetask1", "servicetask2", "servicetask3"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is not waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(),
              "servicetask1, servicetask2, servicetask3");
    }

    @Test
    public void testProcessInstanceWaitingAtNonExistingElementError() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // when
      completeTask("non-existing-task");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isWaitingAt("non-existing-task"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is not waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "non-existing-task");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtError() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotWaitingAt("servicetask1"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(), "servicetask1");
    }

    @Test
    public void testProcessInstanceIsNotWaitingAtMulitpleElementsError() throws InterruptedException {
      // given
      deployProcess("multiple-tasks.bpmn");

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance("multiple-tasks");

      // then
      assertThatThrownBy(() -> assertThat(instanceEvent).isNotWaitingAt("servicetask1", "servicetask2", "servicetask3"))
          .isInstanceOf(AssertionError.class)
          .hasMessage(
              "Process with key %s is waiting at element(s) with id(s) %s",
              instanceEvent.getProcessInstanceKey(),
              "servicetask1, servicetask2, servicetask3");
    }
  }

  private void deployProcess(final String process) {
    client.newDeployCommand().addResourceFromClasspath(process).send().join();
  }

  private ProcessInstanceEvent startProcessInstance(final String processId) throws InterruptedException {
    return startProcessInstance(processId, new HashMap<>());
  }

  private ProcessInstanceEvent startProcessInstance(final String processId, final Map<String, Object> variables)
      throws InterruptedException {
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
}
