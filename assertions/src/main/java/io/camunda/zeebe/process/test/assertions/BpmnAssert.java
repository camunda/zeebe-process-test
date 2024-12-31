/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.process.test.assertions;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.response.ProcessInstanceResult;
import io.camunda.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;

/**
 * This class manages all the entry points for the specific assertions.
 *
 * <p>For example when starting a process instance:
 *
 * <pre>{@code
 * ProcessInstanceEvent event = client.newCreateInstanceCommand()
 *     .bpmnProcessId("processId")
 *     .latestVersion()
 *     .send()
 *     .join();
 * ProcessInstanceAssert assertions = BpmnAssert.assertThat(event);
 * }</pre>
 */
public abstract class BpmnAssert {

  static ThreadLocal<RecordStream> recordStream = new ThreadLocal<>();

  /**
   * Initializes a {@link RecordStream}. The {@link RecordStream} will be stored in a {@link
   * ThreadLocal} and thus will only be accessible in the current thread. This will be managed for
   * you if you're using the @ZeebeProcessTest annotation.
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
   * This will be managed for you if you're using the @ZeebeProcessTest annotation.
   *
   * @return the {@link RecordStream} stored for this thread
   * @throws AssertionError if no {@link RecordStream} has been initialized for the current thread
   */
  public static RecordStream getRecordStream() {
    if (recordStream.get() == null) {
      throw new AssertionError(
          "No RecordStream is set. Please make sure you are using the "
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
