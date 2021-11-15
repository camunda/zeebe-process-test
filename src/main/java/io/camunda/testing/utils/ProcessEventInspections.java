package io.camunda.testing.utils;

import static io.camunda.testing.utils.RecordStreamManager.getRecordStreamSource;

import io.camunda.testing.assertions.ProcessInstanceAssert;
import io.camunda.testing.filters.ProcessEventRecordStreamFilter;
import io.camunda.zeebe.protocol.record.intent.ProcessEventIntent;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;

public class ProcessEventInspections {

  private final ProcessEventRecordStreamFilter filter;

  public ProcessEventInspections(final ProcessEventRecordStreamFilter filter) {
    this.filter = filter;
  }

  public ProcessEventInspections triggeredByTimer(final String timerId) {
    return new ProcessEventInspections(
        filter.withIntent(ProcessEventIntent.TRIGGERED).withTargetElementId(timerId));
  }

  public ProcessEventInspections withProcessDefinitionKey(final long processDefinitionKey) {
    return new ProcessEventInspections(filter.withProcessDefinitionKey(processDefinitionKey));
  }

  public ProcessInstanceAssert assertThatFirstProcessInstance() {
    return assertThatProcessInstance(0);
  }

  public ProcessInstanceAssert assertThatLastProcessInstance() {
    final List<Long> processInstanceKeys = getProcessInstanceKeys();
    return assertThatProcessInstance(processInstanceKeys, processInstanceKeys.size() - 1);
  }

  public ProcessInstanceAssert assertThatProcessInstance(final int times) {
    final List<Long> processInstanceKeys = getProcessInstanceKeys();
    return assertThatProcessInstance(processInstanceKeys, times);
  }

  private ProcessInstanceAssert assertThatProcessInstance(final List<Long> keys, final int index) {
    long processInstanceKey = -1;
    try {
      processInstanceKey = keys.get(index);
    } catch (IndexOutOfBoundsException ex) {
      Assertions.fail("No process instances have been found");
    }
    return new ProcessInstanceAssert(processInstanceKey, getRecordStreamSource());
  }

  private List<Long> getProcessInstanceKeys() {
    return filter.stream()
        .map(record -> record.getValue().getProcessInstanceKey())
        .filter(record -> record != -1)
        .collect(Collectors.toList());
  }
}
