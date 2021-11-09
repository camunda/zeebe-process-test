package io.camunda.testing.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.testing.filters.StreamFilter;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;
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
    final boolean isStarted =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withBpmnElementType(BpmnElementType.PROCESS)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isStarted)
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
            .withBpmnElementType(BpmnElementType.PROCESS)
            .stream()
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
    assertThat(isCompleted(actual.getProcessInstanceKey()))
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
    assertThat(isCompleted(actual.getProcessInstanceKey()))
        .withFailMessage("Process with key %s was completed", actual.getProcessInstanceKey())
        .isFalse();
    return this;
  }

  /**
   * Checks if a process instance has been completed
   *
   * @param processInstanceKey the key of the process instance
   * @return boolean indicating whether the process instance has been completed
   */
  private boolean isCompleted(final long processInstanceKey) {
    return StreamFilter.processInstance(recordStreamSource)
        .withProcessInstanceKey(processInstanceKey)
        .withBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED).stream()
        .findFirst()
        .isPresent();
  }

  /**
   * Verifies the expectation that the process instance is terminated.
   *
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isTerminated() {
    assertThat(isTerminated(actual.getProcessInstanceKey()))
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
    assertThat(isTerminated(actual.getProcessInstanceKey()))
        .withFailMessage("Process with key %s was terminated", actual.getProcessInstanceKey())
        .isFalse();
    return this;
  }

  /**
   * Checks if a process instance has been terminated
   *
   * @param processInstanceKey the key of the process instance
   * @return boolean indicating whether the process instance has been terminated
   */
  private boolean isTerminated(final long processInstanceKey) {
    return StreamFilter.processInstance(recordStreamSource)
        .withProcessInstanceKey(processInstanceKey)
        .withBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_TERMINATED).stream()
        .findFirst()
        .isPresent();
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
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withElementId(elementId)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED)
            .stream()
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
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withElementIdIn(elementIds)
            .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED)
            .stream()
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
   * @param elementIdsVarArg The ids of the elements
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isWaitingAtElement(final String... elementIdsVarArg) {
    final List<String> elementIds = Arrays.asList(elementIdsVarArg);
    final List<String> elementsInWaitState = getElementsInWaitState();

    final List<String> differences = elementIds.stream()
        .filter(element -> !elementsInWaitState.contains(element))
        .collect(Collectors.toList());

    assertThat(elementsInWaitState)
        .withFailMessage(
            "Process with key %s is not waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", differences))
        .containsAll(elementIds);

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently not waiting at one or more
   * specified elements.
   *
   * @param elementIdsVarArg The ids of the elements
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isNotWaitingAtElement(final String... elementIdsVarArg) {
    final List<String> elementIds = Arrays.asList(elementIdsVarArg);
    final List<String> elementsInWaitState = getElementsInWaitState();

    final List<String> similarities = elementIds.stream()
        .filter(elementsInWaitState::contains)
        .collect(Collectors.toList());

    assertThat(elementsInWaitState)
        .withFailMessage(
            "Process with key %s is waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", similarities))
        .doesNotContainAnyElementsOf(elementIds);

    return this;
  }

  /**
   * Gets the elements that are currently in a waiting state.
   *
   * @return list containing the element ids of the elements in a waiting state
   */
  private List<String> getElementsInWaitState() {
    final List<String> elementsInWaitState = new ArrayList<>();
    StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
        .withProcessInstanceKey(actual.getProcessInstanceKey())
        .withoutBpmnElementType(BpmnElementType.PROCESS)
        .stream()
        .collect(Collectors.toMap(
            record -> String.format("%s-%s", record.getValue().getElementId(), record.getValue().getFlowScopeKey()),
            record -> record,
            (existing, replacement) -> replacement))
        .forEach((key, record) -> {
          if (record.getIntent().equals(ProcessInstanceIntent.ELEMENT_ACTIVATED)) {
            elementsInWaitState.add(record.getValue().getElementId());
          }
        });
    return elementsInWaitState;
  }

  /**
   * Verifies the expectation that the process instance is currently waiting at the specified
   * elements, and not at any other element.
   *
   * @param elementIdsVarArg The ids of the elements
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isWaitingExactlyAtElements(final String... elementIdsVarArg) {
    final List<String> elementIds = Arrays.asList(elementIdsVarArg);
    final List<String> elementsInWaitState = getElementsInWaitState();
    final List<String> wrongfullyWaitingElementIds = new ArrayList<>();
    final List<String> wrongfullyNotWaitingElementIds = new ArrayList<>();

    StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
        .withProcessInstanceKey(actual.getProcessInstanceKey())
        .withoutBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .stream()
        .map(Record::getValue)
        .map(ProcessInstanceRecordValue::getElementId)
        .forEach(
            id -> {
              final boolean shouldBeWaitingAtElement = elementIds.contains(id);
              final boolean isWaitingAtElement = elementsInWaitState.contains(id);
              if (shouldBeWaitingAtElement && !isWaitingAtElement) {
                wrongfullyNotWaitingElementIds.add(id);
              } else if (!shouldBeWaitingAtElement && isWaitingAtElement) {
                wrongfullyWaitingElementIds.add(id);
              }
            });

    final SoftAssertions softly = new SoftAssertions();
    softly.assertThat(wrongfullyWaitingElementIds.isEmpty())
        .withFailMessage(
            "Process with key %s is waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", wrongfullyWaitingElementIds))
        .isTrue();
    softly.assertThat(wrongfullyNotWaitingElementIds.isEmpty())
        .withFailMessage(
            "Process with key %s is not waiting at element(s) with id(s) %s",
            actual.getProcessInstanceKey(), String.join(", ", wrongfullyNotWaitingElementIds))
        .isTrue();
    softly.assertAll();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently waiting to receive one or more
   * specified messages.
   *
   * @param messageNamesVarArg Names of the messages
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isWaitingForMessage(final String... messageNamesVarArg) {
    final List<String> messageNames = Arrays.asList(messageNamesVarArg);
    final List<String> openMessageSubscriptions = getOpenMessageSubscriptions();

    final List<String> differences = messageNames.stream()
        .filter(element -> !openMessageSubscriptions.contains(element))
        .collect(Collectors.toList());

    assertThat(openMessageSubscriptions)
        .withFailMessage(
            "Process with key %s is not waiting for message(s) with name(s) %s",
            actual.getProcessInstanceKey(),
            String.join(", ", differences))
        .containsAll(messageNames);

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently not waiting to receive one or
   * more specified messages.
   *
   * @param messageNamesVarArg Names of the messages
   * @return this {@link ProcessInstanceAssertions}
   */
  public ProcessInstanceAssertions isNotWaitingForMessage(final String... messageNamesVarArg) {
    final List<String> messageNames = Arrays.asList(messageNamesVarArg);
    final List<String> openMessageSubscriptions = getOpenMessageSubscriptions();

    final List<String> similarities = messageNames.stream()
        .filter(openMessageSubscriptions::contains)
        .collect(Collectors.toList());

    assertThat(openMessageSubscriptions)
        .withFailMessage(
            "Process with key %s is waiting for message(s) with name(s) %s",
            actual.getProcessInstanceKey(),
            String.join(", ", similarities))
        .doesNotContainAnyElementsOf(messageNames);

    return this;
  }

  /**
   * Gets the currently open message subscription from the record stream source
   *
   * @return list containing the message names of the open message subscriptions
   */
  private List<String> getOpenMessageSubscriptions() {
    final List<String> openMessageSubscriptions = new ArrayList<>();
    StreamFilter.processMessageSubscription(recordStreamSource.processMessageSubscriptionRecords())
        .withProcessInstanceKey(actual.getProcessInstanceKey())
        .stream()
        .collect(Collectors.toMap(
            record -> record.getValue().getElementInstanceKey(),
            record -> record,
            (existing, replacement) -> replacement))
        .forEach((key, record) -> {
          if (record.getIntent().equals(ProcessMessageSubscriptionIntent.CREATING) ||
              record.getIntent().equals(ProcessMessageSubscriptionIntent.CREATED)) {
            openMessageSubscriptions.add(record.getValue().getMessageName());
          }
        });
    return openMessageSubscriptions;
  }
}
