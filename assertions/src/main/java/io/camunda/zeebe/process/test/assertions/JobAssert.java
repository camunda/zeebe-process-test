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

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.zeebe.process.test.filters.IncidentRecordStreamFilter;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.data.Offset;

/** Assertions for {@link ActivatedJob} instances */
public class JobAssert extends AbstractAssert<JobAssert, ActivatedJob> {

  private final RecordStream recordStream;

  public JobAssert(final ActivatedJob actual, final RecordStream recordStream) {
    super(actual, JobAssert.class);
    this.recordStream = recordStream;
  }

  /**
   * Asserts that the activated job is associated to an element with the given id
   *
   * @param expectedElementId element id to check
   * @return this {@link JobAssert}
   */
  public JobAssert hasElementId(final String expectedElementId) {
    assertThat(expectedElementId).describedAs("expectedElementId").isNotNull().isNotEmpty();
    final String actualElementId = actual.getElementId();

    assertThat(actualElementId)
        .withFailMessage(
            "Job is not associated with expected element id '%s' but is instead associated with '%s'.",
            expectedElementId, actualElementId)
        .isEqualTo(expectedElementId);
    return this;
  }

  /**
   * Asserts that the activated job has the given deadline
   *
   * @param expectedDeadline deadline in terms of {@code System.currentTimeMillis()}
   * @param offset offset in milliseconds to tolerate timing invariances
   * @return this {@link JobAssert}
   */
  public JobAssert hasDeadline(final long expectedDeadline, final Offset<Long> offset) {
    assertThat(offset).describedAs("Offset").isNotNull();
    final long actualDeadline = actual.getDeadline();

    assertThat(actualDeadline).describedAs("Deadline").isCloseTo(expectedDeadline, offset);
    return this;
  }

  /**
   * Asserts that the activated job is associated to the given process id
   *
   * @param expectedBpmnProcessId proces id to check
   * @return this {@link JobAssert}
   */
  public JobAssert hasBpmnProcessId(final String expectedBpmnProcessId) {
    assertThat(expectedBpmnProcessId).describedAs("expectedBpmnProcessId").isNotNull().isNotEmpty();
    final String actualBpmnProcessId = actual.getBpmnProcessId();

    assertThat(actualBpmnProcessId)
        .withFailMessage(
            "Job is not associated with BPMN process id '%s' but is instead associated with '%s'.",
            expectedBpmnProcessId, actualBpmnProcessId)
        .isEqualTo(expectedBpmnProcessId);
    return this;
  }

  /**
   * Asserts that the activated job has the given number of retries
   *
   * @param expectedRetries expected retries
   * @return this {@link JobAssert}
   */
  public JobAssert hasRetries(final int expectedRetries) {
    final int actualRetries = actual.getRetries();

    assertThat(actualRetries)
        .withFailMessage(
            "Job does not have %d retries, as expected, but instead has %d retries.",
            expectedRetries, actualRetries)
        .isEqualTo(expectedRetries);
    return this;
  }

  /**
   * Asserts whether any incidents were raised for this job (regardless of whether these incidents
   * are active or already resolved)
   *
   * @return this {@link JobAssert}
   */
  public JobAssert hasAnyIncidents() {
    final boolean incidentsWereRaised =
        getIncidentCreatedRecords().stream().findFirst().isPresent();

    assertThat(incidentsWereRaised)
        .withFailMessage("No incidents were raised for this job")
        .isTrue();
    return this;
  }

  /**
   * Asserts whether no incidents were raised for this job
   *
   * @return this {@link JobAssert}
   */
  public JobAssert hasNoIncidents() {
    final boolean incidentsWereRaised =
        getIncidentCreatedRecords().stream().findFirst().isPresent();

    assertThat(incidentsWereRaised).withFailMessage("Incidents were raised for this job").isFalse();
    return this;
  }

  /**
   * Extracts the latest incident
   *
   * @return {@link IncidentAssert} for the latest incident
   */
  public IncidentAssert extractingLatestIncident() {
    hasAnyIncidents();

    final List<Record<IncidentRecordValue>> incidentCreatedRecords =
        getIncidentCreatedRecords().stream().collect(Collectors.toList());

    final Record<IncidentRecordValue> latestIncidentRecord =
        incidentCreatedRecords.get(incidentCreatedRecords.size() - 1);

    return new IncidentAssert(latestIncidentRecord.getKey(), recordStream);
  }

  private IncidentRecordStreamFilter getIncidentCreatedRecords() {
    return StreamFilter.incident(recordStream)
        .withRejectionType(RejectionType.NULL_VAL)
        .withJobKey(actual.getKey());
  }

  /**
   * Extracts the variables of the activated job.
   *
   * @return this {@link JobAssert}
   */
  public MapAssert<String, Object> extractingVariables() {
    return assertThat(actual.getVariablesAsMap()).describedAs("Variables");
  }

  /**
   * Extracts the header values of the activated job.
   *
   * @return this {@link JobAssert}
   */
  public MapAssert<String, String> extractingHeaders() {
    return assertThat(actual.getCustomHeaders()).describedAs("Headers");
  }
}
