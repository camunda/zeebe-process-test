package io.camunda.testing.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.data.Offset;

// TODO discuss name: job assertions, service task assertions, worker assertions

/** Assertions for {@code ActivatedJob} instances */
public class JobAssert extends AbstractAssert<JobAssert, ActivatedJob> {

  public JobAssert(final ActivatedJob actual) {
    super(actual, JobAssert.class);
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
        .overridingErrorMessage(
            "Job is not associated with expected element id '%s' but is instead associated with '%s'",
            expectedElementId, actualElementId)
        .isEqualTo(expectedElementId);
    return this;
  }

  // TODO decide whether this assertion has any value

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
        .overridingErrorMessage(
            "Job is not associated with BPMN process id '%s' but is instead associated with '%s'",
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
        .overridingErrorMessage(
            "Job does not have %d retries but instead %d", expectedRetries, actualRetries)
        .isEqualTo(expectedRetries);
    return this;
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
