package io.camunda.zeebe.process.test.qa.inspections;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.inspections.InspectionUtility;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.process.test.qa.util.Utilities;
import io.camunda.zeebe.process.test.qa.util.Utilities.ProcessPackCallActivity;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@ZeebeProcessTest
public class ProcessInstanceInspectionsTest {

  private ZeebeClient client;
  private InMemoryEngine engine;

  @Test
  public void testStartedByProcessInstanceWithProcessId() {
    // given
    Utilities.deployProcesses(
        client,
        ProcessPackCallActivity.RESOURCE_NAME,
        ProcessPackCallActivity.CALLED_RESOURCE_NAME);
    final ProcessInstanceEvent instanceEvent =
        Utilities.startProcessInstance(engine, client, ProcessPackCallActivity.PROCESS_ID);

    // when
    final Optional<InspectedProcessInstance> firstProcessInstance =
        InspectionUtility.findProcessInstances()
            .withParentProcessInstanceKey(instanceEvent.getProcessInstanceKey())
            .withBpmnProcessId(ProcessPackCallActivity.CALLED_PROCESS_ID)
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isNotEmpty();
    BpmnAssert.assertThat(firstProcessInstance.get()).isCompleted();
    BpmnAssert.assertThat(instanceEvent)
        .hasPassedElement(ProcessPackCallActivity.CALL_ACTIVITY_ID)
        .isCompleted();
  }

  @Test
  public void testStartedByProcessInstanceWithProcessId_wrongId() {
    // given
    Utilities.deployProcesses(
        client,
        ProcessPackCallActivity.RESOURCE_NAME,
        ProcessPackCallActivity.CALLED_RESOURCE_NAME);
    final ProcessInstanceEvent instanceEvent =
        Utilities.startProcessInstance(engine, client, ProcessPackCallActivity.PROCESS_ID);

    // when
    final Optional<InspectedProcessInstance> firstProcessInstance =
        InspectionUtility.findProcessInstances()
            .withParentProcessInstanceKey(instanceEvent.getProcessInstanceKey())
            .withBpmnProcessId("wrongId")
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isEmpty();
  }
}
