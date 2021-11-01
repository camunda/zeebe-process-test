package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.ProcessInstanceAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.util.Collections;
import org.camunda.community.eze.EmbeddedZeebeEngine;
import org.camunda.community.eze.RecordStreamSource;
import org.junit.jupiter.api.Test;

// TODO remove Thread.sleeps
@EmbeddedZeebeEngine
class ProcessInstanceAssertionsTest {

  private static final String PROCESS_ID = "processinstance";
  private static final String ELEMENT_ID = "servicetask";

  private ZeebeClient client;
  private RecordStreamSource recordStreamSource;

  @Test
  public void testProcessInstanceIsStarted() throws InterruptedException {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThat(instanceEvent, recordStreamSource).isStarted();
  }

  @Test
  public void testProcessInstanceIsNotStarted() {
    // given
    deployProcess();
    final ProcessInstanceEvent mockInstanceEvent = mock(ProcessInstanceEvent.class);

    // when
    when(mockInstanceEvent.getProcessInstanceKey()).thenReturn(-1L);

    // then
    assertThrows(AssertionError.class, assertThat(mockInstanceEvent, recordStreamSource)::isStarted,
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
    assertThrows(AssertionError.class, assertThat(mockInstanceEvent, recordStreamSource)::isStarted,
        "Process with key -1 was not started");
  }

  @Test
  public void testProcessInstanceIsCompleted() throws InterruptedException {
    // given
    deployProcess();
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // when
    completeTask();

    // then
    assertThat(instanceEvent, recordStreamSource).isCompleted();
  }

  @Test
  public void testProcessInstanceNotCompleted() throws InterruptedException {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThrows(AssertionError.class, assertThat(instanceEvent, recordStreamSource)::isCompleted,
        String.format("Process with key %s was not started", instanceEvent.getProcessInstanceKey()));
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
    assertThat(instanceEvent, recordStreamSource).isTerminated();
  }

  @Test
  public void testProcessInstanceNotTerminated() throws InterruptedException {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThrows(AssertionError.class, assertThat(instanceEvent, recordStreamSource)::isTerminated,
        String.format("Process with key %s was not terminated", instanceEvent.getProcessInstanceKey()));
  }

  @Test
  public void testProcessInstanceHasPassed() throws InterruptedException {
    // given
    deployProcess();
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // when
    completeTask();

    // then
    assertThat(instanceEvent, recordStreamSource).hasPassed(ELEMENT_ID);
  }

  @Test
  public void testProcessInstanceHasPassedError() throws InterruptedException {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThrows(AssertionError.class,
        () -> assertThat(instanceEvent, recordStreamSource).hasPassed(ELEMENT_ID),
        String.format("Expected element with id %s to be passed 1 times", ELEMENT_ID));
  }

  @Test
  public void testProcessInstanceHasNotPassed() throws InterruptedException {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThat(instanceEvent, recordStreamSource).hasNotPassed(ELEMENT_ID);
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
        () -> assertThat(instanceEvent, recordStreamSource).hasNotPassed(ELEMENT_ID),
        String.format("Expected element with id %s to be passed 0 times", ELEMENT_ID));
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
    assertThat(instanceEvent, recordStreamSource).hasPassed(ELEMENT_ID, totalLoops);
  }

  @Test
  public void testProcessInstanceIsWaitingAt() throws InterruptedException {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThat(instanceEvent, recordStreamSource).isWaitingAt(ELEMENT_ID);
  }

  @Test
  public void testProcessInstanceIsNotWaitingAt() throws InterruptedException {
    // given
    deployProcess();
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // when
    completeTask();

    // then
    assertThrows(AssertionError.class, () -> assertThat(instanceEvent, recordStreamSource)
            .isWaitingAt("servicetask"),
        String.format("Process with key %s is not waiting at element with id %s",
            instanceEvent.getProcessInstanceKey(), ELEMENT_ID));
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

  private ProcessInstanceEvent startProcessInstance(final int totalLoops) throws InterruptedException {
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
    for (Record<JobRecordValue> record : recordStreamSource.jobRecords().withElementId(ELEMENT_ID)) {
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
