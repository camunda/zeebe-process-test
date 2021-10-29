package io.camunda.testing.assertions;

import static io.camunda.testing.assertions.ProcessInstanceAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.camunda.community.eze.EmbeddedZeebeEngine;
import org.camunda.community.eze.RecordStreamSource;
import org.junit.jupiter.api.Test;

@EmbeddedZeebeEngine
class ProcessInstanceAssertionsTest {

  private ZeebeClient client;
  private RecordStreamSource recordStreamSource;

  @Test
  public void testProcessInstanceIsStarted() {
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
  public void testProcessInstanceIsNotStartedIfProcessInstanceKeyNoMatch() {
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
    completeTask("servicetask");

    // then
    assertThat(instanceEvent, recordStreamSource).isCompleted();
  }

  @Test
  public void testProcessInstanceNotCompleted() {
    // given
    deployProcess();

    // when
    final ProcessInstanceEvent instanceEvent = startProcessInstance();

    // then
    assertThrows(AssertionError.class, assertThat(instanceEvent, recordStreamSource)::isCompleted,
        String.format("Process with key %s was not started", instanceEvent.getProcessInstanceKey()));
  }

  private void deployProcess() {
    client.newDeployCommand()
        .addResourceFromClasspath("process-instance.bpmn")
        .send()
        .join();
  }

  private ProcessInstanceEvent startProcessInstance() {
    return client.newCreateInstanceCommand()
        .bpmnProcessId("processinstance")
        .latestVersion()
        .send()
        .join();
  }

  //TODO remove Thread.sleeps
  private void completeTask(final String elementId) throws InterruptedException {
    Thread.sleep(100);
    recordStreamSource.jobRecords()
        .withElementId(elementId)
        .forEach(record ->
            client.newCompleteCommand(record.getKey())
                .send()
                .join());
    Thread.sleep(100);
  }
}
