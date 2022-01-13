package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.filters.IncidentRecordStreamFilter;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.process.test.testengine.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.value.ErrorType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.Optional;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.StringAssert;

/** Assertions for incidents. An incident is identified by its incident key. */
public class IncidentAssert extends AbstractAssert<IncidentAssert, Long> {
  private final String LINE_SEPARATOR = System.lineSeparator();

  private final RecordStreamSource recordStreamSource;

  public IncidentAssert(final long incidentKey, final RecordStreamSource recordStreamSource) {
    super(incidentKey, IncidentAssert.class);
    this.recordStreamSource = recordStreamSource;
  }

  /**
   * Returns the incident key (useful for resolving the incident)
   *
   * @return key of the incident
   */
  public long getIncidentKey() {
    return actual;
  }

  /**
   * Asserts that the incident has the given error type
   *
   * @param expectedErrorType expected error type
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert hasErrorType(final ErrorType expectedErrorType) {
    assertThat(expectedErrorType).describedAs("Parameter 'expectedErrorType").isNotNull();
    final IncidentRecordValue record = getIncidentCreatedRecordValue();
    final ErrorType actualErrorType = record.getErrorType();

    assertThat(actualErrorType)
        .withFailMessage(
            "Error type was not '%s' but was '%s' instead.%s",
            expectedErrorType, actualErrorType, composeIncidentDetails())
        .isEqualTo(expectedErrorType);

    return this;
  }

  /**
   * Asserts that the incident has the given error message
   *
   * @param expectedErrorMessage expected error message
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert hasErrorMessage(final String expectedErrorMessage) {
    final IncidentRecordValue record = getIncidentCreatedRecordValue();
    final String actualErrorMessage = record.getErrorMessage();

    assertThat(actualErrorMessage)
        .withFailMessage(
            "Error message was not '%s' but was '%s' instead.%s",
            expectedErrorMessage, actualErrorMessage, composeIncidentDetails())
        .isEqualTo(expectedErrorMessage);

    return this;
  }

  /**
   * Extracts the error message for further assertions
   *
   * @return {@link StringAssert} of error message
   */
  public StringAssert extractErrorMessage() {
    final IncidentRecordValue record = getIncidentCreatedRecordValue();
    final String actualErrorMessage = record.getErrorMessage();

    return new StringAssert(actualErrorMessage);
  }

  /**
   * Asserts that the incident is associated with the given process instance
   *
   * @param expectedProcessInstance expected process instance
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert wasRaisedInProcessInstance(
      final ProcessInstanceEvent expectedProcessInstance) {
    assertThat(expectedProcessInstance).isNotNull();
    return wasRaisedInProcessInstance(expectedProcessInstance.getProcessInstanceKey());
  }

  /**
   * Asserts that the incident is associated with the given process instance
   *
   * @param expectedProcessInstanceKey key of expected process instance
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert wasRaisedInProcessInstance(final long expectedProcessInstanceKey) {
    final IncidentRecordValue record = getIncidentCreatedRecordValue();
    final long actualProcessInstanceKey = record.getProcessInstanceKey();

    assertThat(actualProcessInstanceKey)
        .withFailMessage(
            "Incident was not raised in process instance %d but was raised in %s instead.%s",
            expectedProcessInstanceKey, actualProcessInstanceKey, composeIncidentDetails())
        .isEqualTo(expectedProcessInstanceKey);

    return this;
  }

  /**
   * Asserts that the incident is associated with the given element
   *
   * @param expectedElementId id of the element on which the incident was raised
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert occurredOnElement(final String expectedElementId) {
    assertThat(expectedElementId).isNotEmpty();

    final IncidentRecordValue record = getIncidentCreatedRecordValue();
    final String actualElementId = record.getElementId();

    assertThat(actualElementId)
        .withFailMessage(
            "Error type was not raised on element '%s' but was raised on '%s' instead.%s",
            expectedElementId, actualElementId, composeIncidentDetails())
        .isEqualTo(expectedElementId);

    return this;
  }

  /**
   * Asserts that the incident is associated with the given job
   *
   * @param expectedJob job during which the incident was raised
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert occurredDuringJob(final ActivatedJob expectedJob) {
    assertThat(expectedJob).isNotNull();
    return occurredDuringJob(expectedJob.getKey());
  }

  /**
   * Asserts that the incident is associated with the given job
   *
   * @param expectedJobKey koy of job during which the incident was raised
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert occurredDuringJob(final long expectedJobKey) {
    final IncidentRecordValue record = getIncidentCreatedRecordValue();
    final long actualJobKey = record.getJobKey();

    assertThat(actualJobKey)
        .withFailMessage(
            "Incident was not raised during job instance %d but was raised in %s instead.%s",
            expectedJobKey, actualJobKey, composeIncidentDetails())
        .isEqualTo(expectedJobKey);

    return this;
  }

  /**
   * Asserts that the incident is resolved
   *
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert isResolved() {
    final boolean resolved = isIncidentResolved();

    assertThat(resolved)
        .withFailMessage("Incident is not resolved." + composeIncidentDetails())
        .isTrue();
    return this;
  }

  /**
   * Asserts that the incident is not resolved
   *
   * @return this {@link IncidentAssert}
   */
  public IncidentAssert isUnresolved() {
    final boolean resolved = isIncidentResolved();

    assertThat(resolved)
        .withFailMessage("Incident is already resolved." + composeIncidentDetails())
        .isFalse();
    return this;
  }

  private IncidentRecordStreamFilter getIncidentRecords(final IncidentIntent intent) {
    return StreamFilter.incident(recordStreamSource)
        .withRejectionType(RejectionType.NULL_VAL)
        .withIncidentKey(actual)
        .withIntent(intent);
  }

  private IncidentRecordValue getIncidentCreatedRecordValue() {
    final Optional<Record<IncidentRecordValue>> optIncidentCreatedRecord =
        findIncidentCreatedRecord();

    assertThat(optIncidentCreatedRecord)
        .describedAs("Incident created record for key %d.%s", actual, composeIncidentDetails())
        .isPresent();

    return optIncidentCreatedRecord.get().getValue();
  }

  private Optional<Record<IncidentRecordValue>> findIncidentCreatedRecord() {
    return getIncidentRecords(IncidentIntent.CREATED).stream().findFirst();
  }

  private boolean isIncidentResolved() {
    return getIncidentRecords(IncidentIntent.RESOLVED).stream().findFirst().isPresent();
  }

  private String composeIncidentDetails() {
    final Optional<Record<IncidentRecordValue>> optRecord = findIncidentCreatedRecord();

    if (!optRecord.isPresent()) {
      return LINE_SEPARATOR + "No incident details found for key " + actual;
    } else {
      final Record<IncidentRecordValue> record = optRecord.get();

      final StringBuilder result = new StringBuilder();
      result
          .append(LINE_SEPARATOR + "Incident[")
          .append(LINE_SEPARATOR + "  key: ")
          .append(record.getKey())
          .append(LINE_SEPARATOR + "  errorType: ")
          .append((record.getValue().getErrorType()))
          .append(LINE_SEPARATOR + "  errorMessage: \"")
          .append(record.getValue().getErrorMessage())
          .append("\"");

      final String elementId = record.getValue().getElementId();

      if (elementId != null) {
        result.append(LINE_SEPARATOR + "  elementId: ").append(elementId);
      }

      final long jobKey = record.getValue().getJobKey();

      if (jobKey != -1) {
        result.append(LINE_SEPARATOR + "  jobKey: ").append(jobKey);
      }

      result.append(LINE_SEPARATOR + "]");
      return result.toString();
    }
  }
}
