package io.camunda.testing.assertions;

import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.camunda.community.eze.RecordStreamSource;

public class ProcessInstanceAssertions
    extends AbstractAssert<ProcessInstanceAssertions, ProcessInstanceEvent> {

  private RecordStreamSource recordStreamSource;

  public ProcessInstanceAssertions(final ProcessInstanceEvent actual) {
    super(actual, ProcessInstanceAssertions.class);
    this.recordStreamSource = BpmnAssertions.recordStreamSource.get();
    if (recordStreamSource == null) {
      failWithMessage(
          "No RecordStreamSource is set. Please use the @ZeebeAssertions "
              + "extenstion and declare a RecordStreamSource field");
    }
  }

  public static ProcessInstanceAssertions assertThat(final ProcessInstanceEvent actual) {
    return new ProcessInstanceAssertions(actual);
  }

  public ProcessInstanceAssertions isStarted() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS).stream()
            .findFirst();

    if (!processInstanceRecord.isPresent()) {
      failWithMessage("Process with key %s was not started", actual.getProcessInstanceKey());
    }

    return this;
  }

  public ProcessInstanceAssertions isCompleted() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED).stream()
            .findFirst();

    if (!processInstanceRecord.isPresent()) {
      failWithMessage("Process with key %s was not completed", actual.getProcessInstanceKey());
    }

    return this;
  }

  public ProcessInstanceAssertions isTerminated() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .withIntent(ProcessInstanceIntent.ELEMENT_TERMINATED).stream()
            .findFirst();

    if (!processInstanceRecord.isPresent()) {
      failWithMessage("Process with key %s was not terminated", actual.getProcessInstanceKey());
    }

    return this;
  }

  public ProcessInstanceAssertions hasPassed(final String elementId) {
    return hasPassed(elementId, 1);
  }

  public ProcessInstanceAssertions hasNotPassed(final String elementId) {
    return hasPassed(elementId, 0);
  }

  public ProcessInstanceAssertions hasPassed(final String elementId, final int times) {
    final long count =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withElementId(elementId)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED).stream()
            .count();

    if (count != times) {
      failWithActualExpectedAndMessage(
          count, times, "Expected element with id %s to be passed %s times", elementId, times);
    }

    return this;
  }

  public ProcessInstanceAssertions isWaitingAt(final String elementId) {
    final List<Record<ProcessInstanceRecordValue>> elementProcessInstanceRecords =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withElementId(elementId)
            .stream()
            .collect(Collectors.toList());
    final Record<ProcessInstanceRecordValue> lastRecord =
        elementProcessInstanceRecords.get(elementProcessInstanceRecords.size() - 1);

    if (lastRecord.getIntent() != ProcessInstanceIntent.ELEMENT_ACTIVATED) {
      failWithMessage(
          "Process with key %s is not waiting at element with id %s",
          actual.getProcessInstanceKey(), elementId);
    }

    return this;
  }

  public ProcessInstanceAssertions isNotWaitingAt(final String elementId) {
    final List<Record<ProcessInstanceRecordValue>> elementProcessInstanceRecords =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withElementId(elementId)
            .stream()
            .collect(Collectors.toList());
    final Record<ProcessInstanceRecordValue> lastRecord =
        elementProcessInstanceRecords.get(elementProcessInstanceRecords.size() - 1);

    if (lastRecord.getIntent() == ProcessInstanceIntent.ELEMENT_ACTIVATED) {
      failWithMessage(
          "Process with key %s is waiting at element with id %s",
          actual.getProcessInstanceKey(), elementId);
    }

    return this;
  }
}
