package io.camunda.testing.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.testing.filters.StreamFilter;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.ArrayList;
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

  public ProcessInstanceAssertions(
      final ProcessInstanceEvent actual, final RecordStreamSource recordStreamSource) {
    super(actual, ProcessInstanceAssertions.class);
    this.recordStreamSource = recordStreamSource;
  }

  /**
   * Verifies the expectation that the process instance is started. This will also be true when the
   * process has been completed or terminated.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isStarted() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS).stream()
            .findFirst();

    assertThat(processInstanceRecord.isPresent())
        .withFailMessage("Process with key %s was not started", actual.getProcessInstanceKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is active.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isActive() {
    final boolean isActive =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS).stream()
            .noneMatch(
                record ->
                    record.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED
                        || record.getIntent() == ProcessInstanceIntent.ELEMENT_TERMINATED);

    assertThat(isActive)
        .withFailMessage("Process with key %s is not active", actual.getProcessInstanceKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is completed.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isCompleted() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED).stream()
            .findFirst();

    assertThat(processInstanceRecord.isPresent())
        .withFailMessage("Process with key %s was not completed", actual.getProcessInstanceKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is not completed.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isNotCompleted() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED).stream()
            .findFirst();

    assertThat(processInstanceRecord.isPresent())
        .withFailMessage("Process with key %s was completed", actual.getProcessInstanceKey())
        .isFalse();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is terminated.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isTerminated() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .withIntent(ProcessInstanceIntent.ELEMENT_TERMINATED).stream()
            .findFirst();

    assertThat(processInstanceRecord.isPresent())
        .withFailMessage("Process with key %s was not terminated", actual.getProcessInstanceKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is not terminated.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isNotTerminated() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .withIntent(ProcessInstanceIntent.ELEMENT_TERMINATED).stream()
            .findFirst();

    assertThat(processInstanceRecord.isPresent())
        .withFailMessage("Process with key %s was terminated", actual.getProcessInstanceKey())
        .isFalse();

    return this;
  }

  /**
   * Verifies the expectation that the process instance has passed an element with a specific
   * element id exactly one time.
   *
   * @param elementId The id of the element
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions hasPassedElement(final String elementId) {
    return hasPassedElement(elementId, 1);
  }

  /**
   * Verifies the expectation that the process instance has not passed an element with a specific
   * element id.
   *
   * @param elementId The id of the element
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions hasNotPassedElement(final String elementId) {
    return hasPassedElement(elementId, 0);
  }

  /**
   * Verifies the expectation that the process instance has passed an element with a specific
   * element id exactly N amount of times.
   *
   * @param elementId The id of the element
   * @param times The amount of times the element should be passed
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions hasPassedElement(final String elementId, final int times) {
    final long count =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withElementId(elementId)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED).stream()
            .count();

    assertThat(count)
        .withFailMessage("Expected element with id %s to be passed %s times", elementId, times)
        .isEqualTo(times);

    return this;
  }

  /**
   * Verifies the expectation that the process instance has passed the given elements in order.
   *
   * @param elementIds The element ids
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions hasPassedElementInOrder(final String... elementIds) {
    final List<String> foundElementRecords =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withElementIdIn(elementIds)
            .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATING).stream()
            .map(Record::getValue)
            .map(ProcessInstanceRecordValue::getElementId)
            .collect(Collectors.toList());

    assertThat(foundElementRecords)
        .describedAs("Ordered elements")
        .isEqualTo(Arrays.asList(elementIds));

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently waiting at one or more
   * specified elements.
   *
   * @param elementIds The ids of the elements
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isWaitingAtElement(final String... elementIds) {
    final List<String> notActivatedElements =
        Arrays.stream(elementIds)
            .filter(((Predicate<String>) this::isWaitingAtElement).negate())
            .collect(Collectors.toList());

    assertThat(notActivatedElements.isEmpty())
        .withFailMessage(
            "Process with key %s is not waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", notActivatedElements))
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently not waiting at one or more
   * specified elements.
   *
   * @param elementIds The ids of the elements
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isNotWaitingAtElement(final String... elementIds) {
    final List<String> activatedElements =
        Arrays.stream(elementIds).filter(this::isWaitingAtElement).collect(Collectors.toList());

    assertThat(activatedElements.isEmpty())
        .withFailMessage(
            "Process with key %s is waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", activatedElements))
        .isTrue();

    return this;
  }

  /**
   * Checks if the process instance is currently waiting at an element with a specific element id.
   *
   * @param elementId The id of the element
   * @return boolean indicating whether the process instance is waiting at the element
   */
  // TODO if waiting at any element with this id returns true. Maybe add a counter so we can check
  // we are waiting at multiple elements with the same id
  private boolean isWaitingAtElement(final String elementId) {
    final Map<Long, List<Record<ProcessInstanceRecordValue>>> recordsMap =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withElementId(elementId)
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

  /**
   * Verifies the expectation that the process instance is currently waiting at the specified
   * elements, and not at any other element.
   *
   * @param elementIds The ids of the elements
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isWaitingExactlyAtElements(final String... elementIds) {
    final List<String> shouldBeWaitingAt = Arrays.asList(elementIds);
    final List<String> wrongfullyWaitingElementIds = new ArrayList<>();
    final List<String> wrongfullyNotWaitingElementIds = new ArrayList<>();

    StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
        .withProcessInstanceKey(actual.getProcessInstanceKey())
        .withoutBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATING).stream()
        .map(Record::getValue)
        .map(ProcessInstanceRecordValue::getElementId)
        .forEach(
            id -> {
              if (shouldBeWaitingAt.contains(id) && !isWaitingAtElement(id)) {
                // not waiting for the element we want to wait for!
                wrongfullyNotWaitingElementIds.add(id);
              } else if (!shouldBeWaitingAt.contains(id) && isWaitingAtElement(id)) {
                // waiting for an element we don't want to wait for!
                wrongfullyWaitingElementIds.add(id);
              }
            });

    assertThat(wrongfullyWaitingElementIds.isEmpty())
        .withFailMessage(
            "Process with key %s is waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", wrongfullyWaitingElementIds))
        .isTrue();
    assertThat(wrongfullyNotWaitingElementIds.isEmpty())
        .withFailMessage(
            "Process with key %s is not waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", wrongfullyNotWaitingElementIds))
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently waiting to receive one or more
   * specified messages.
   *
   * @param messageNames Names of the messages
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isWaitingForMessage(final String... messageNames) {
    final List<String> notCreatedProcessMessageSubscriptions =
        Arrays.stream(messageNames)
            .filter(((Predicate<String>) this::isWaitingForMessage).negate())
            .collect(Collectors.toList());

    assertThat(notCreatedProcessMessageSubscriptions.isEmpty())
        .withFailMessage(
            "Process with key %s is not waiting for message(s) with name(s) %s",
            actual.getProcessInstanceKey(),
            String.join(", ", notCreatedProcessMessageSubscriptions))
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently not waiting to receive one or
   * more specified messages.
   *
   * @param messageNames Names of the messages
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isNotWaitingForMessage(final String... messageNames) {
    final List<String> createdProcessMessageSubscriptions =
        Arrays.stream(messageNames).filter(this::isWaitingForMessage).collect(Collectors.toList());

    assertThat(createdProcessMessageSubscriptions.isEmpty())
        .withFailMessage(
            "Process with key %s is waiting for message(s) with name(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", createdProcessMessageSubscriptions))
        .isTrue();

    return this;
  }

  /**
   * Checks if the process instance is currently waiting for a message with a specific message name.
   *
   * @param messageName The name of the message
   * @return boolean indicating whether the process instance is waiting for the message
   */
  private boolean isWaitingForMessage(final String messageName) {
    final Map<Long, List<Record<ProcessMessageSubscriptionRecordValue>>> recordsMap =
        StreamFilter.processMessageSubscription(
                recordStreamSource.processMessageSubscriptionRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey()).withMessageName(messageName)
            .stream()
            .collect(Collectors.groupingBy(record -> record.getValue().getElementInstanceKey()));

    if (recordsMap.isEmpty()) {
      return false;
    }

    for (Entry<Long, List<Record<ProcessMessageSubscriptionRecordValue>>> entry :
        recordsMap.entrySet()) {
      final Record<ProcessMessageSubscriptionRecordValue> lastRecord =
          entry.getValue().get(entry.getValue().size() - 1);
      final Intent intent = lastRecord.getIntent();
      if (intent.equals(ProcessMessageSubscriptionIntent.CREATING)
          || intent.equals(ProcessMessageSubscriptionIntent.CREATED)) {
        return true;
      }
    }

    return false;
  }
}
