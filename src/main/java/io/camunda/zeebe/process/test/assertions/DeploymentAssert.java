package io.camunda.zeebe.process.test.assertions;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.Process;
import io.camunda.zeebe.process.test.testengine.RecordStreamSource;
import java.util.List;
import org.assertj.core.api.AbstractAssert;

/** Assertions for {@code DeploymentEvent} instances */
public class DeploymentAssert extends AbstractAssert<DeploymentAssert, DeploymentEvent> {

  private final RecordStreamSource recordStreamSource;

  public DeploymentAssert(
      final DeploymentEvent actual, final RecordStreamSource recordStreamSource) {
    super(actual, DeploymentAssert.class);
    this.recordStreamSource = recordStreamSource;
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

    return new ProcessAssert(matchingProcesses.get(0), recordStreamSource);
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

    return new ProcessAssert(matchingProcesses.get(0), recordStreamSource);
  }
}
