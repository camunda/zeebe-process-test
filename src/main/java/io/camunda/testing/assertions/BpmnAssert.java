package io.camunda.testing.assertions;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import org.camunda.community.eze.RecordStreamSource;

public abstract class BpmnAssert {

  static ThreadLocal<RecordStreamSource> recordStreamSource = new ThreadLocal<>();

  public static void init(final RecordStreamSource recordStreamSource) {
    BpmnAssert.recordStreamSource.set(recordStreamSource);
  }

  public static void reset() {
    recordStreamSource.remove();
  }

  public static ProcessInstanceAssert assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssert(instanceEvent, getRecordStreamSource());
  }

  public static JobAssert assertThat(final ActivatedJob activatedJob) {
    return new JobAssert(activatedJob);
  }

  public static DeploymentAssert assertThat(final DeploymentEvent deploymentEvent) {
    return new DeploymentAssert(deploymentEvent, getRecordStreamSource());
  }

  public static MessageAssert assertThat(final PublishMessageResponse publishMessageResponse) {
    return new MessageAssert(publishMessageResponse, getRecordStreamSource());
  }

  private static RecordStreamSource getRecordStreamSource() {
    if (recordStreamSource.get() == null) {
      throw new AssertionError(
          "No RecordStreamSource is set. Please use @ZeebeAssertions extension and "
              + "declare a RecordStreamSource field");
    }
    return recordStreamSource.get();
  }
}
