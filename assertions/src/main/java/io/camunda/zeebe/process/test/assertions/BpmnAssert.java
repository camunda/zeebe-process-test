package io.camunda.zeebe.process.test.assertions;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;

public abstract class BpmnAssert {

  static ThreadLocal<RecordStreamSource> recordStreamSource = new ThreadLocal<>();

  public static void initRecordStream(final RecordStreamSource recordStreamSource) {
    BpmnAssert.recordStreamSource.set(recordStreamSource);
  }

  public static void resetRecordStream() {
    recordStreamSource.remove();
  }

  public static RecordStreamSource getRecordStreamSource() {
    if (recordStreamSource.get() == null) {
      throw new AssertionError(
          "No RecordStreamSource is set. Please make sure you are using the "
              + "@ZeebeProcessTest annotation. Alternatively, set one manually using "
              + "BpmnAssert.initRecordStream.");
    }
    return recordStreamSource.get();
  }

  public static ProcessInstanceAssert assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssert(
        instanceEvent.getProcessInstanceKey(), getRecordStreamSource());
  }

  public static ProcessInstanceAssert assertThat(final ProcessInstanceResult instanceResult) {
    return new ProcessInstanceAssert(instanceResult.getProcessInstanceKey(),
        getRecordStreamSource());
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
