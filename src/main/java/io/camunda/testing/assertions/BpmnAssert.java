package io.camunda.testing.assertions;

import static io.camunda.testing.utils.RecordStreamSourceStore.getRecordStreamSource;

import io.camunda.testing.utils.model.InspectedProcessInstance;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;

public abstract class BpmnAssert {

  public static ProcessInstanceAssert assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssert(
        instanceEvent.getProcessInstanceKey(), getRecordStreamSource());
  }

  public static ProcessInstanceAssert assertThat(
      final InspectedProcessInstance inspectedProcessInstance) {
    return new ProcessInstanceAssert(
        inspectedProcessInstance.getProcessInstanceKey(), getRecordStreamSource());
  }

  public static JobAssert assertThat(final ActivatedJob activatedJob) {
    return new JobAssert(activatedJob, getRecordStreamSource());
  }

  public static DeploymentAssert assertThat(final DeploymentEvent deploymentEvent) {
    return new DeploymentAssert(deploymentEvent, getRecordStreamSource());
  }

  public static MessageAssert assertThat(final PublishMessageResponse publishMessageResponse) {
    return new MessageAssert(publishMessageResponse, getRecordStreamSource());
  }
}
