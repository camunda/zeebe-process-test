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

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.Form;
import io.camunda.zeebe.client.api.response.Process;
import io.camunda.zeebe.process.test.filters.RecordStream;
import java.util.List;
import org.assertj.core.api.AbstractAssert;

/** Assertions for {@link DeploymentEvent} instances */
public class DeploymentAssert extends AbstractAssert<DeploymentAssert, DeploymentEvent> {

  private final RecordStream recordStream;

  public DeploymentAssert(final DeploymentEvent actual, final RecordStream recordStream) {
    super(actual, DeploymentAssert.class);
    this.recordStream = recordStream;
  }

  /**
   * Asserts that the deployment contains the given BPMN process IDs
   *
   * @param expectedBpmnProcessIds BPMN process IDs to check
   * @return this {@link DeploymentAssert}
   */
  public DeploymentAssert containsProcessesByBpmnProcessId(final String... expectedBpmnProcessIds) {
    assertThat(expectedBpmnProcessIds).isNotEmpty();

    final List<String> deployedProcesses =
        actual.getProcesses().stream().map(Process::getBpmnProcessId).collect(toList());

    assertThat(deployedProcesses)
        .describedAs("Deployed Processes (BPMN process IDs)")
        .contains(expectedBpmnProcessIds);

    return this;
  }

  /**
   * Asserts that the deployment contains processes with the given resources
   *
   * @param expectedProcessInstanceResourceNames resource names to check
   * @return this {@link DeploymentAssert}
   */
  public DeploymentAssert containsProcessesByResourceName(
      final String... expectedProcessInstanceResourceNames) {
    assertThat(expectedProcessInstanceResourceNames).isNotEmpty();

    final List<String> deployedProcesses =
        actual.getProcesses().stream().map(Process::getResourceName).collect(toList());

    assertThat(deployedProcesses)
        .describedAs("Deployed Processes (resource name)")
        .contains(expectedProcessInstanceResourceNames);

    return this;
  }

  /**
   * Extracts the process with the given BPMN process ID
   *
   * @param bpmnProcessId BPMN process ID to look up
   * @return this {@link JobAssert}
   */
  public ProcessAssert extractingProcessByBpmnProcessId(final String bpmnProcessId) {
    assertThat(bpmnProcessId).describedAs("Parameter 'bpmnProcessId'").isNotEmpty();

    final List<Process> matchingProcesses =
        actual.getProcesses().stream()
            .filter(process -> process.getBpmnProcessId().equals(bpmnProcessId))
            .collect(toList());

    assertThat(matchingProcesses)
        .withFailMessage(
            "Expected to find one process for BPMN process id '%s' but found %d: %s",
            bpmnProcessId, matchingProcesses.size(), matchingProcesses)
        .hasSize(1);

    return new ProcessAssert(matchingProcesses.get(0), recordStream);
  }

  /**
   * Extracts the process with the given resource name
   *
   * @param resourceName resource name to look up
   * @return this {@link JobAssert}
   */
  public ProcessAssert extractingProcessByResourceName(final String resourceName) {
    assertThat(resourceName).describedAs("Parameter 'resourceName'").isNotEmpty();

    final List<Process> matchingProcesses =
        actual.getProcesses().stream()
            .filter(process -> process.getResourceName().equals(resourceName))
            .collect(toList());

    assertThat(matchingProcesses)
        .withFailMessage(
            "Expected to find one process for resource name '%s' but found %d: %s",
            resourceName, matchingProcesses.size(), matchingProcesses)
        .hasSize(1);

    return new ProcessAssert(matchingProcesses.get(0), recordStream);
  }

  /**
   * Extracts the form with the given form ID
   *
   * @param formId form ID to look up
   * @return this {@link JobAssert}
   */
  public FormAssert extractingFormByFormId(final String formId) {
    assertThat(formId).describedAs("Parameter 'formId'").isNotEmpty();

    final List<Form> matchingForm =
        actual.getForm().stream().filter(form -> form.getFormId().equals(formId)).collect(toList());

    assertThat(matchingForm)
        .withFailMessage(
            "Expected to find one form for formId '%s' but found %d: %s",
            formId, matchingForm.size(), matchingForm)
        .hasSize(1);

    return new FormAssert(matchingForm.get(0), recordStream);
  }

  /**
   * Extracts the form with the given resource name
   *
   * @param resourceName resource name to look up
   * @return this {@link JobAssert}
   */
  public FormAssert extractingFormByResourceName(final String resourceName) {
    assertThat(resourceName).describedAs("Parameter 'resourceName'").isNotEmpty();

    final List<Form> matchingForm =
        actual.getForm().stream()
            .filter(form -> form.getResourceName().equals(resourceName))
            .collect(toList());

    assertThat(matchingForm)
        .withFailMessage(
            "Expected to find one form for resource name '%s' but found %d: %s",
            resourceName, matchingForm.size(), matchingForm)
        .hasSize(1);

    return new FormAssert(matchingForm.get(0), recordStream);
  }
}
