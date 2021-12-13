package io.camunda.zeebe.process.test.assertions;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.RecordStreamSourceStore;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;

public abstract class BpmnAssert {

  public static ProcessInstanceAssert assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssert(
        instanceEvent.getProcessInstanceKey(), RecordStreamSourceStore.getRecordStreamSource());
  }

  public static ProcessInstanceAssert assertThat(
      final InspectedProcessInstance inspectedProcessInstance) {
    return new ProcessInstanceAssert(
        inspectedProcessInstance.getProcessInstanceKey(),
        RecordStreamSourceStore.getRecordStreamSource());
  }

  public static JobAssert assertThat(final ActivatedJob activatedJob) {
    return new JobAssert(activatedJob, RecordStreamSourceStore.getRecordStreamSource());
  }

  public static DeploymentAssert assertThat(final DeploymentEvent deploymentEvent) {
    return new DeploymentAssert(deploymentEvent, RecordStreamSourceStore.getRecordStreamSource());
  }

  public static MessageAssert assertThat(final PublishMessageResponse publishMessageResponse) {
    return new MessageAssert(
        publishMessageResponse, RecordStreamSourceStore.getRecordStreamSource());
  }
}
