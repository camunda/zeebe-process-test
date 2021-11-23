package io.camunda.testing.utils;

import static io.camunda.testing.assertions.BpmnAssert.assertThat;
import static io.camunda.testing.util.Utilities.deployProcesses;
import static io.camunda.testing.util.Utilities.startProcessInstance;
import static io.camunda.testing.utils.InspectionUtility.findProcessInstances;

import io.camunda.testing.extensions.ZeebeAssertions;
import io.camunda.testing.util.Utilities.ProcessPackCallActivity;
import io.camunda.testing.utils.model.InspectedProcessInstance;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.camunda.community.eze.RecordStreamSource;
import org.camunda.community.eze.ZeebeEngine;
import org.junit.jupiter.api.Test;

@ZeebeAssertions
public class ProcessInstanceInspectionsTest {

  private ZeebeClient client;
  private ZeebeEngine engine;
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
            .startedByProcessInstance(instanceEvent.getProcessInstanceKey())
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
            .startedByProcessInstance(instanceEvent.getProcessInstanceKey())
            .withBpmnProcessId("wrongId")
            .findFirstProcessInstance();

    // then
    Assertions.assertThat(firstProcessInstance).isEmpty();
  }
}
