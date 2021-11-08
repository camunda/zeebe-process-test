package io.camunda.testing.assertions;

import io.camunda.zeebe.client.api.response.Process;
import org.assertj.core.api.AbstractAssert;
import org.camunda.community.eze.RecordStreamSource;

public class ProcessAssert extends AbstractAssert<ProcessAssert, Process> {

  private final RecordStreamSource recordStreamSource;

  public ProcessAssert(final Process actual, final RecordStreamSource recordStreamSource) {
    super(actual, ProcessAssert.class);
    this.recordStreamSource = recordStreamSource;
  }

  public ProcessAssert hasBPMNProcessId(final String expectedBpmnProcessId) {
    return this;
  }

  public ProcessAssert hasVersion(final long expectedVersion) {
    return this;
  }

  public ProcessAssert hasResourceName(final String expectedResourceName) {
    return this;
  }

  public ProcessAssert hasAnyInstances() {
    return this;
  }

  public ProcessAssert hasInstances(final long expectedNumberOfInstances) {
    return this;
  }

  public ProcessAssert hasAnyActiveInstances() {
    return this;
  }

  public ProcessAssert hasActiveInstances(final long expectedNumberOfActiveInstances) {
    return this;
  }

  public ProcessAssert extractPreviousVersion() {
    return this;
  }

  public ProcessInstanceAssertions extractFirstInstance() {
    return null;
  }

  public ProcessInstanceAssertions extractLastInstance() {
    return null;
  }
}
