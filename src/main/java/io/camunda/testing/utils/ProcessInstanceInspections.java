package io.camunda.testing.utils;

import io.camunda.testing.filters.ProcessInstanceRecordStreamFilter;
import io.camunda.testing.utils.model.InspectedProcessInstance;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProcessInstanceInspections {

  private final ProcessInstanceRecordStreamFilter filter;

  public ProcessInstanceInspections(final ProcessInstanceRecordStreamFilter filter) {
    this.filter = filter;
  }

  /**
   * Filters the process instances to only include instances that were started by the given parent
   * process instance key
   *
   * @param key The key of the parent process instance
   * @return this {@link ProcessInstanceInspections}
   */
  public ProcessInstanceInspections startedByProcessInstance(final long key) {
    return new ProcessInstanceInspections(filter.withParentProcessInstanceKey(key));
  }

  /**
   * Filters the process instances to only include instances that have the given BPMN process id
   *
   * @param bpmnProcessId The BPMN process id of the desired process instance
   * @return this {@link ProcessInstanceInspections}
   */
  public ProcessInstanceInspections withBpmnProcessId(final String bpmnProcessId) {
    return new ProcessInstanceInspections(filter.withBpmnProcessId(bpmnProcessId));
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
