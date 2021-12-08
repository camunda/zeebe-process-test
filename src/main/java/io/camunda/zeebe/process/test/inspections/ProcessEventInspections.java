package io.camunda.zeebe.process.test.inspections;

import io.camunda.zeebe.process.test.filters.ProcessEventRecordStreamFilter;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import io.camunda.zeebe.protocol.record.intent.ProcessEventIntent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProcessEventInspections {

  private final ProcessEventRecordStreamFilter filter;

  public ProcessEventInspections(final ProcessEventRecordStreamFilter filter) {
    this.filter = filter;
  }

  /**
   * Filters the process instances to only include instances that were triggered by a given timer
   * event id
   *
   * @param timerId The element ID of the timer event
   * @return this {@link ProcessEventInspections}
   */
  public ProcessEventInspections triggeredByTimer(final String timerId) {
    return new ProcessEventInspections(
        filter.withIntent(ProcessEventIntent.TRIGGERED).withTargetElementId(timerId));
  }

  /**
   * Filters the process instances to only include instance with a given process definition key
   *
   * @param processDefinitionKey The process definition key
   * @return this {@link ProcessEventInspections}
   */
  public ProcessEventInspections withProcessDefinitionKey(final long processDefinitionKey) {
    return new ProcessEventInspections(filter.withProcessDefinitionKey(processDefinitionKey));
  }

  /**
   * Finds the first process instance that matches the applied filters
   *
   * @return {@link Optional} of {@link InspectedProcessInstance}
   */
  public Optional<InspectedProcessInstance> findFirstProcessInstance() {
    return findProcessInstance(0);
  }

  /**
   * Finds the last process instance that matches the applied filters
   *
   * @return {@link Optional} of {@link InspectedProcessInstance}
   */
  public Optional<InspectedProcessInstance> findLastProcessInstance() {
    final List<Long> processInstanceKeys = getProcessInstanceKeys();
    return findProcessInstance(processInstanceKeys, processInstanceKeys.size() - 1);
  }

  /**
   * Finds the process instance that matches the applied filters at a given index
   *
   * <p>Example: If 3 process instances have been started, findProcessInstance(1) would return the
   * second started instance.
   *
   * @param index The index of the process instance start at 0
   * @return {@link Optional} of {@link InspectedProcessInstance}
   */
  public Optional<InspectedProcessInstance> findProcessInstance(final int index) {
    final List<Long> processInstanceKeys = getProcessInstanceKeys();
    return findProcessInstance(processInstanceKeys, index);
  }

  /**
   * Gets the given index from a list of process instance keys uses it to create an {@link
   * InspectedProcessInstance}
   *
   * @param keys The list of process instance key
   * @param index The desired index
   * @return {@link Optional} of {@link InspectedProcessInstance}
   */
  private Optional<InspectedProcessInstance> findProcessInstance(
      final List<Long> keys, final int index) {
    try {
      final long processInstanceKey = keys.get(index);
      return Optional.of(new InspectedProcessInstance(processInstanceKey));
    } catch (IndexOutOfBoundsException ex) {
      return Optional.empty();
    }
  }

  /**
   * Maps the filtered stream to a list of process instance keys
   *
   * @return List of process instance keys
   */
  private List<Long> getProcessInstanceKeys() {
    return filter.stream()
        .map(record -> record.getValue().getProcessInstanceKey())
        .filter(record -> record != -1)
        .collect(Collectors.toList());
  }
}
