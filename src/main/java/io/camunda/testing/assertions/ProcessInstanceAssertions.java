package io.camunda.testing.assertions;

import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
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

  public ProcessInstanceAssertions isWaitingAt(final String... elementIds) {
    final List<String> activatedElements = Arrays.stream(elementIds)
        .filter(((Predicate<String>) this::isWaitingAtElement).negate())
        .collect(Collectors.toList());

    if (!activatedElements.isEmpty()) {
      final String errorMessage =
          String.format("Process with key %s is not waiting at element(s) with id(s) %s",
              actual.getProcessInstanceKey(), String.join(", ", activatedElements));
      failWithMessage(errorMessage);
    }
    return this;
  }

  public ProcessInstanceAssertions isNotWaitingAt(final String... elementIds) {
    final List<String> activatedElements = Arrays.stream(elementIds)
        .filter(this::isWaitingAtElement)
        .collect(Collectors.toList());

    if (!activatedElements.isEmpty()) {
      final String errorMessage =
          String.format("Process with key %s is waiting at element(s) with id(s) %s",
              actual.getProcessInstanceKey(), String.join(", ", activatedElements));
      failWithMessage(errorMessage);
    }
    return this;
  }

  // TODO if waiting at any element with this id returns true. Maybe add a counter so we can check
  // we are waiting at multiple elements with the same id
  private boolean isWaitingAtElement(final String elementId) {
    final Map<Long, List<Record<ProcessInstanceRecordValue>>> recordsMap =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withElementId(elementId)
            .stream()
            .collect(Collectors.groupingBy(record -> record.getValue().getFlowScopeKey()));

    if (recordsMap.isEmpty()) {
      return false;
    }

    for (Entry<Long, List<Record<ProcessInstanceRecordValue>>> entry : recordsMap.entrySet()) {
      final Record<ProcessInstanceRecordValue> lastRecord =
          entry.getValue().get(entry.getValue().size() - 1);
      if (lastRecord.getIntent() == ProcessInstanceIntent.ELEMENT_ACTIVATED) {
        return true;
      }
    }

    return false;
  }
}
