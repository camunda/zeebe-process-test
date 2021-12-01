package io.camunda.zeebe.bpmnassert.inspections;

import static io.camunda.zeebe.bpmnassert.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.bpmnassert.inspections.InspectionUtility.findProcessInstances;
import static io.camunda.zeebe.bpmnassert.util.Utilities.deployProcesses;
import static io.camunda.zeebe.bpmnassert.util.Utilities.startProcessInstance;

import io.camunda.zeebe.bpmnassert.extensions.ZeebeProcessTest;
import io.camunda.zeebe.bpmnassert.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.bpmnassert.testengine.InMemoryEngine;
import io.camunda.zeebe.bpmnassert.testengine.RecordStreamSource;
import io.camunda.zeebe.bpmnassert.util.Utilities.ProcessPackCallActivity;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
public class ProcessInstanceInspectionsTest {

  private ZeebeClient client;
  private InMemoryEngine engine;
  private RecordStreamSource recordStreamSource;

  @Test
  public void testStartedByProcessInstanceWithProcessId() {
    // given
    deployProcesses(
        client,
        ProcessPackCallActivity.RESOURCE_NAME,
        ProcessPackCallActivity.CALLED_RESOURCE_NAME);
    final ProcessInstanceEvent instanceEvent =
        startProcessInstance(engine, client, ProcessPackCallActivity.PROCESS_ID);

    // when
    final Optional<InspectedProcessInstance> firstProcessInstance =
        findProcessInstances()
            .withParentProcessInstanceKey(instanceEvent.getProcessInstanceKey())
            .withBpmnProcessId(ProcessPackCallActivity.CALLED_PROCESS_ID)
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isNotEmpty();
    assertThat(firstProcessInstance.get()).isCompleted();
    assertThat(instanceEvent)
        .hasPassedElement(ProcessPackCallActivity.CALL_ACTIVITY_ID)
        .isCompleted();
  }

  @Test
  public void testStartedByProcessInstanceWithProcessId_wrongId() {
    // given
    deployProcesses(
        client,
        ProcessPackCallActivity.RESOURCE_NAME,
        ProcessPackCallActivity.CALLED_RESOURCE_NAME);
    final ProcessInstanceEvent instanceEvent =
        startProcessInstance(engine, client, ProcessPackCallActivity.PROCESS_ID);

    // when
    final Optional<InspectedProcessInstance> firstProcessInstance =
        findProcessInstances()
            .withParentProcessInstanceKey(instanceEvent.getProcessInstanceKey())
            .withBpmnProcessId("wrongId")
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isEmpty();
  }
}
