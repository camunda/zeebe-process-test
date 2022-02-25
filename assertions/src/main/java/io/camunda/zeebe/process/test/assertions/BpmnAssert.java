package io.camunda.zeebe.process.test.assertions;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;

public abstract class BpmnAssert {

  static ThreadLocal<RecordStream> recordStream = new ThreadLocal<>();

  public static void initRecordStream(final RecordStream recordStream) {
    BpmnAssert.recordStream.set(recordStream);
  }

  public static void resetRecordStream() {
    recordStream.remove();
  }

  public static RecordStream getRecordStream() {
    if (recordStream.get() == null) {
      throw new AssertionError(
          "No RecordStreamSource is set. Please make sure you are using the "
              + "@ZeebeProcessTest annotation. Alternatively, set one manually using "
              + "BpmnAssert.initRecordStream.");
    }
    return recordStream.get();
  }

  public static ProcessInstanceAssert assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssert(
        instanceEvent.getProcessInstanceKey(), getRecordStream());
  }

  public static ProcessInstanceAssert assertThat(final ProcessInstanceResult instanceResult) {
    return new ProcessInstanceAssert(
        instanceResult.getProcessInstanceKey(), getRecordStream());
  }

  public static ProcessInstanceAssert assertThat(
      final InspectedProcessInstance inspectedProcessInstance) {
    return new ProcessInstanceAssert(
        inspectedProcessInstance.getProcessInstanceKey(), getRecordStream());
  }

  public static JobAssert assertThat(final ActivatedJob activatedJob) {
    return new JobAssert(activatedJob, getRecordStream());
  }

  public static DeploymentAssert assertThat(final DeploymentEvent deploymentEvent) {
    return new DeploymentAssert(deploymentEvent, getRecordStream());
  }

  public static MessageAssert assertThat(final PublishMessageResponse publishMessageResponse) {
    return new MessageAssert(publishMessageResponse, getRecordStream());
  }
}
