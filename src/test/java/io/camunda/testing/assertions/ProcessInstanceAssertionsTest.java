package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.ProcessInstanceAssertions.assertThat;
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
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// TODO remove Thread.sleeps
@ZeebeAssertions
class ProcessInstanceAssertionsTest {

  private static final String PROCESS_ID = "processinstance";
  private static final String ELEMENT_ID = "servicetask";

  private ZeebeClient client;
  private ZeebeEngine engine;

  // These tests are for testing assertions as well as examples for users
  @Nested
  class HappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testProcessInstanceIsStarted() throws InterruptedException {
      // given
      deployProcess();

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // then
      assertThat(instanceEvent).isStarted();
    }

    @Test
    public void testProcessInstanceIsCompleted() throws InterruptedException {
      // given
      deployProcess();
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // when
      completeTask();

      // then
      assertThat(instanceEvent).isCompleted();
    }

    @Test
    public void testProcessInstanceTerminated() throws InterruptedException {
      // given
      deployProcess();
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // when
      client.newCancelInstanceCommand(instanceEvent.getProcessInstanceKey()).send().join();
      Thread.sleep(100);

      // then
      assertThat(instanceEvent).isTerminated();
    }

    @Test
    public void testProcessInstanceHasPassed() throws InterruptedException {
      // given
      deployProcess();
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // when
      completeTask();

      // then
      assertThat(instanceEvent).hasPassed(ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasNotPassed() throws InterruptedException {
      // given
      deployProcess();

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // then
      assertThat(instanceEvent).hasNotPassed(ELEMENT_ID);
    }

    @Test
    public void testProcessInstanceHasPassedMultipleTimes() throws InterruptedException {
      // given
      deployProcess();
      final int totalLoops = 5;
      final ProcessInstanceEvent instanceEvent = startProcessInstance(totalLoops);

      // when
      for (int i = 0; i < 5; i++) {
        completeTask();
      }

      // then
      assertThat(instanceEvent).hasPassed(ELEMENT_ID, totalLoops);
    }

    @Test
    public void testProcessInstanceIsWaitingAt() throws InterruptedException {
      // given
      deployProcess();

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // then
      assertThat(instanceEvent).isWaitingAt(ELEMENT_ID);
    }
  }

  // These tests are just for assertion testing purposes. These should not be used as examples.
  @Nested
  class UnhappyPathTests {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testProcessInstanceIsNotStarted() {
      // given
      deployProcess();
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      assertThrows(AssertionError.class, assertThat(mockInstanceEvent)::isStarted,
          "Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceIsNotStartedIfProcessInstanceKeyNoMatch()
        throws InterruptedException {
      // given
      deployProcess();
      startProcessInstance();
      final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

      // when
      when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

      // then
      assertThrows(AssertionError.class, assertThat(mockInstanceEvent)::isStarted,
          "Process with key -1 was not started");
    }

    @Test
    public void testProcessInstanceNotCompleted() throws InterruptedException {
      // given
      deployProcess();

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // then
      assertThrows(AssertionError.class, assertThat(instanceEvent)::isCompleted,
          String.format("Process with key %s was not started",
              instanceEvent.getProcessInstanceKey()));
    }

    @Test
    public void testProcessInstanceNotTerminated() throws InterruptedException {
      // given
      deployProcess();

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // then
      assertThrows(AssertionError.class, assertThat(instanceEvent)::isTerminated,
          String.format("Process with key %s was not terminated",
              instanceEvent.getProcessInstanceKey()));
    }

    @Test
    public void testProcessInstanceHasPassedError() throws InterruptedException {
      // given
      deployProcess();

      // when
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // then
      assertThrows(AssertionError.class,
          () -> assertThat(instanceEvent).hasPassed(ELEMENT_ID),
          String.format("Expected element with id %s to be passed 1 times", ELEMENT_ID));
    }

    @Test
    public void testProcessInstanceHasNotPassedError() throws InterruptedException {
      // given
      deployProcess();
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // when
      completeTask();

      // then
      assertThrows(AssertionError.class,
          () -> assertThat(instanceEvent).hasNotPassed(ELEMENT_ID),
          String.format("Expected element with id %s to be passed 0 times", ELEMENT_ID));
    }

    @Test
    public void testProcessInstanceIsNotWaitingAt() throws InterruptedException {
      // given
      deployProcess();
      final ProcessInstanceEvent instanceEvent = startProcessInstance();

      // when
      completeTask();

      // then
      assertThrows(AssertionError.class, () -> assertThat(instanceEvent)
              .isWaitingAt("servicetask"),
          String.format("Process with key %s is not waiting at element with id %s",
              instanceEvent.getProcessInstanceKey(), ELEMENT_ID));
    }
  }

  private void deployProcess() {
    client.newDeployCommand()
        .addResourceFromClasspath("process-instance.bpmn")
        .send()
        .join();
  }

  private ProcessInstanceEvent startProcessInstance() throws InterruptedException {
    return startProcessInstance(1);
  }

  private ProcessInstanceEvent startProcessInstance(final int totalLoops)
      throws InterruptedException {
    final ProcessInstanceEvent instanceEvent = client.newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(Collections.singletonMap("totalLoops", totalLoops))
        .send()
        .join();
    Thread.sleep(100);
    return instanceEvent;
  }

  // TODO we need a proper way to complete jobs instead of this hack
  private void completeTask() throws InterruptedException {
    Thread.sleep(100);
    Record<JobRecordValue> lastRecord = null;
    for (Record<JobRecordValue> record : engine.jobRecords()
        .withElementId(ELEMENT_ID)) {
      if (record.getIntent().equals(JobIntent.CREATED)) {
        lastRecord = record;
      }
    }
    if (lastRecord != null) {
      client.newCompleteCommand(lastRecord.getKey())
          .send()
          .join();
    }
    Thread.sleep(100);
  }
}
