package io.camunda.zeebe.process.test.assertions;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;

/** This class manages all the entry points for the specific assertions. */
public abstract class BpmnAssert {

  static ThreadLocal<RecordStream> recordStream = new ThreadLocal<>();

  /**
   * Initializes a {@link RecordStream}. The {@link RecordStream} will be stored in a {@link
   * ThreadLocal} and thus will only be accessible in the current thread.
   *
   * @param recordStream the {@link RecordStream}
   */
  public static void initRecordStream(final RecordStream recordStream) {
    BpmnAssert.recordStream.set(recordStream);
  }

  /** Removes the {@link RecordStream} from the {@link ThreadLocal}. */
  public static void resetRecordStream() {
    recordStream.remove();
  }

  /**
   * Gets the {@link RecordStream} that is stored in the {@link ThreadLocal} for the current thread.
   *
   * @return the {@link RecordStream} stored for this thread
   * @throws AssertionError if no {@link RecordStream} has been initialized for the current thread
   */
  public static RecordStream getRecordStream() {
    if (recordStream.get() == null) {
      throw new AssertionError(
          "No RecordStreamSource is set. Please make sure you are using the "
              + "@ZeebeProcessTest annotation. Alternatively, set one manually using "
              + "BpmnAssert.initRecordStream.");
    }
    return recordStream.get();
  }

  /**
   * Creates a new instance of {@link ProcessInstanceAssert}.
   *
   * @param instanceEvent the event received when starting a process instance
   * @return the created assertion object
   */
  public static ProcessInstanceAssert assertThat(final ProcessInstanceEvent instanceEvent) {
    return new ProcessInstanceAssert(instanceEvent.getProcessInstanceKey(), getRecordStream());
  }

  /**
   * Creates a new instance of {@link ProcessInstanceAssert}.
   *
   * @param instanceResult the event received when starting a process instance
   * @return the created assertion object
   */
  public static ProcessInstanceAssert assertThat(final ProcessInstanceResult instanceResult) {
    return new ProcessInstanceAssert(instanceResult.getProcessInstanceKey(), getRecordStream());
  }

  /**
   * Creates a new instance of {@link ProcessInstanceAssert}.
   *
   * @param inspectedProcessInstance the {@link InspectedProcessInstance} received from the {@link
   *     io.camunda.zeebe.process.test.inspections.ProcessInstanceInspections}
   * @return the created assertion object
   */
  public static ProcessInstanceAssert assertThat(
      final InspectedProcessInstance inspectedProcessInstance) {
    return new ProcessInstanceAssert(
        inspectedProcessInstance.getProcessInstanceKey(), getRecordStream());
  }

  /**
   * Creates a new instance of {@link JobAssert}.
   *
   * @param activatedJob the response received when activating a job
   * @return the created assertion object
   */
  public static JobAssert assertThat(final ActivatedJob activatedJob) {
    return new JobAssert(activatedJob, getRecordStream());
  }

  /**
   * Creates a new instance of {@link DeploymentAssert}.
   *
   * @param deploymentEvent the event received when deploying a process
   * @return the created assertion object
   */
  public static DeploymentAssert assertThat(final DeploymentEvent deploymentEvent) {
    return new DeploymentAssert(deploymentEvent, getRecordStream());
  }

  /**
   * Creates a new instance of {@link PublishMessageResponse}.
   *
   * @param publishMessageResponse the response received when publishing a message
   * @return the created assertion object
   */
  public static MessageAssert assertThat(final PublishMessageResponse publishMessageResponse) {
    return new MessageAssert(publishMessageResponse, getRecordStream());
  }
}
