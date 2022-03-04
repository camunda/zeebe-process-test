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

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.Process;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.AbstractAssert;

/**
 * Assertions for {@link Process} instances.
 *
 * <p>These asserts can be obtained via:
 *
 * <pre>{@code
 * final DeploymentEvent deploymentEvent =
 *         client.newDeployCommand().addResourceFile(file).send().join();
 *
 * final ProcessAssert processAssert =
 *         assertThat(deploymentEvent)
 *             .extractingProcessByBpmnProcessId(PROCESS_ID);
 * }</pre>
 */
public class ProcessAssert extends AbstractAssert<ProcessAssert, Process> {

  private final RecordStream recordStream;

  public ProcessAssert(final Process actual, final RecordStream recordStream) {
    super(actual, ProcessAssert.class);
    this.recordStream = recordStream;
  }

  /**
   * Asserts that the process has the given BPMN process ID
   *
   * @param expectedBpmnProcessId BPMN process IDs to check
   * @return this {@link ProcessAssert}
   */
  public ProcessAssert hasBPMNProcessId(final String expectedBpmnProcessId) {
    assertThat(expectedBpmnProcessId).isNotEmpty();

    final String actualBpmnProcessId = actual.getBpmnProcessId();

    assertThat(actualBpmnProcessId)
        .withFailMessage(
            "Expected BPMN process ID to be '%s' but was '%s' instead.",
            expectedBpmnProcessId, actualBpmnProcessId)
        .isEqualTo(expectedBpmnProcessId);
    return this;
  }

  /**
   * Asserts that the process has the given version
   *
   * @param expectedVersion version to check
   * @return this {@link ProcessAssert}
   */
  public ProcessAssert hasVersion(final long expectedVersion) {
    final long actualVersion = actual.getVersion();

    assertThat(actualVersion)
        .withFailMessage(
            "Expected version to be %d but was %d instead", expectedVersion, actualVersion)
        .isEqualTo(expectedVersion);
    return this;
  }

  /**
   * Asserts that the process has the given resource name
   *
   * @param expectedResourceName resource name to check
   * @return this {@link ProcessAssert}
   */
  public ProcessAssert hasResourceName(final String expectedResourceName) {
    assertThat(expectedResourceName).isNotEmpty();

    final String actualResourceName = actual.getResourceName();

    assertThat(actualResourceName)
        .withFailMessage(
            "Expected resource name to be '%s' but was '%s' instead.",
            expectedResourceName, actualResourceName)
        .isEqualTo(expectedResourceName);

    return this;
  }

  /**
   * Asserts that the process has (had) any instances
   *
   * @return this {@link ProcessAssert}
   */
  public ProcessAssert hasAnyInstances() {
    final boolean logContainsRecordsForThisProcess = getRecords().findFirst().isPresent();

    assertThat(logContainsRecordsForThisProcess)
        .withFailMessage("The process has no instances")
        .isTrue();

    return this;
  }

  /**
   * Asserts that the process has (had) no instances
   *
   * @return this {@link ProcessAssert}
   */
  public ProcessAssert hasNoInstances() {
    final boolean logContainsRecordsForThisProcess = getRecords().findFirst().isPresent();

    assertThat(logContainsRecordsForThisProcess)
        .withFailMessage("The process does have instances")
        .isFalse();

    return this;
  }

  /**
   * Asserts that the process has (had) the given number of instances
   *
   * @param expectedNumberOfInstances number of instances to check
   * @return this {@link ProcessAssert}
   */
  public ProcessAssert hasInstances(final long expectedNumberOfInstances) {
    final long actualNumberOfInstances = getRecordsByProcessInstanceId().size();

    assertThat(actualNumberOfInstances)
        .withFailMessage(
            "Expected number of instances to be %d but was %d instead",
            expectedNumberOfInstances, actualNumberOfInstances)
        .isEqualTo(expectedNumberOfInstances);
    return this;
  }

  private Stream<Record<ProcessInstanceRecordValue>> getRecords() {
    return StreamFilter.processInstance(recordStream)
        .withRejectionType(RejectionType.NULL_VAL)
        .withElementId(actual.getBpmnProcessId())
        .withBpmnElementType(BpmnElementType.PROCESS)
        .stream();
  }

  private Map<Long, List<Record<ProcessInstanceRecordValue>>> getRecordsByProcessInstanceId() {
    return getRecords()
        .collect(Collectors.groupingBy(record -> record.getValue().getProcessInstanceKey()));
  }
}
