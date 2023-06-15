/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.process.test.filters.IncidentRecordStreamFilter;
import io.camunda.zeebe.process.test.filters.ProcessInstanceRecordStreamFilter;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;

/**
 * Assertions for process instances. A process instance is identified by its process instance key.
 */
public class ProcessInstanceAssert extends AbstractAssert<ProcessInstanceAssert, Long> {

  private final RecordStream recordStream;

  public ProcessInstanceAssert(final long actual, final RecordStream recordStream) {
    super(actual, ProcessInstanceAssert.class);
    this.recordStream = recordStream;
  }

  /**
   * Verifies the expectation that the process instance is started. This will also be true when the
   * process has been completed or terminated.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isStarted() {
    final boolean isStarted =
        StreamFilter.processInstance(recordStream)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .withBpmnElementType(BpmnElementType.PROCESS)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isStarted).withFailMessage("Process with key %s was not started", actual).isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is active.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isActive() {
    final boolean isActive =
        StreamFilter.processInstance(recordStream)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
            .withBpmnElementType(BpmnElementType.PROCESS)
            .stream()
            .noneMatch(
                record ->
                    record.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED
                        || record.getIntent() == ProcessInstanceIntent.ELEMENT_TERMINATED);

    assertThat(isActive).withFailMessage("Process with key %s is not active", actual).isTrue();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is completed.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isCompleted() {
    assertThat(isProcessInstanceCompleted())
        .withFailMessage("Process with key %s was not completed", actual)
        .isTrue();
    return this;
  }

  /**
   * Verifies the expectation that the process instance is not completed.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isNotCompleted() {
    assertThat(isProcessInstanceCompleted())
        .withFailMessage("Process with key %s was completed", actual)
        .isFalse();
    return this;
  }

  /**
   * Checks if a process instance has been completed
   *
   * @return boolean indicating whether the process instance has been completed
   */
  private boolean isProcessInstanceCompleted() {
    return StreamFilter.processInstance(recordStream)
        .withProcessInstanceKey(actual)
        .withRejectionType(RejectionType.NULL_VAL)
        .withBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_COMPLETED)
        .stream()
        .findFirst()
        .isPresent();
  }

  /**
   * Verifies the expectation that the process instance is terminated.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isTerminated() {
    assertThat(isProcessInstanceTerminated())
        .withFailMessage("Process with key %s was not terminated", actual)
        .isTrue();
    return this;
  }

  /**
   * Verifies the expectation that the process instance is not terminated.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isNotTerminated() {
    assertThat(isProcessInstanceTerminated())
        .withFailMessage("Process with key %s was terminated", actual)
        .isFalse();
    return this;
  }

  /**
   * Checks if a process instance has been terminated
   *
   * @return boolean indicating whether the process instance has been terminated
   */
  private boolean isProcessInstanceTerminated() {
    return StreamFilter.processInstance(recordStream)
        .withProcessInstanceKey(actual)
        .withRejectionType(RejectionType.NULL_VAL)
        .withBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_TERMINATED)
        .stream()
        .findFirst()
        .isPresent();
  }

  /**
   * Verifies the expectation that the process instance has passed an element with a specific
   * element id exactly one time.
   *
   * @param elementId The id of the element
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasPassedElement(final String elementId) {
    return hasPassedElement(elementId, 1);
  }

  /**
   * Verifies the expectation that the process instance has not passed an element with a specific
   * element id.
   *
   * @param elementId The id of the element
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasNotPassedElement(final String elementId) {
    return hasPassedElement(elementId, 0);
  }

  /**
   * Verifies the expectation that the process instance has passed an element with a specific
   * element id exactly N amount of times.
   *
   * @param elementId The id of the element
   * @param times The amount of times the element should be passed
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasPassedElement(final String elementId, final int times) {
    final long count =
        StreamFilter.processInstance(recordStream)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
            .withElementId(elementId)
            .withIntents(
                ProcessInstanceIntent.ELEMENT_COMPLETED, ProcessInstanceIntent.SEQUENCE_FLOW_TAKEN)
            .stream()
            .count();

    assertThat(count)
        .withFailMessage(
            "Expected element with id %s to be passed %s times, but was %s",
            elementId, times, count)
        .isEqualTo(times);

    return this;
  }

  /**
   * Verifies the expectation that the process instance has passed the given elements in order.
   *
   * @param elementIds The element ids
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasPassedElementsInOrder(final String... elementIds) {
    final List<String> foundElementRecords =
        StreamFilter.processInstance(recordStream)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
            .withElementIdIn(elementIds)
            .withIntents(
                ProcessInstanceIntent.ELEMENT_COMPLETED, ProcessInstanceIntent.SEQUENCE_FLOW_TAKEN)
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
   * @param elementIds The ids of the elements
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isWaitingAtElements(final String... elementIds) {
    final Set<String> elementsInWaitState = getElementsInWaitState();
    assertThat(elementsInWaitState).containsAll(Arrays.asList(elementIds));
    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently not waiting at one or more
   * specified elements.
   *
   * @param elementIds The ids of the elements
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isNotWaitingAtElements(final String... elementIds) {
    final Set<String> elementsInWaitState = getElementsInWaitState();
    assertThat(elementsInWaitState).doesNotContainAnyElementsOf(Arrays.asList(elementIds));
    return this;
  }

  /**
   * Gets the elements that are currently in a waiting state.
   *
   * @return set containing the element ids of the elements in a waiting state
   */
  private Set<String> getElementsInWaitState() {
    final Set<String> elementsInWaitState = new HashSet<>();
    StreamFilter.processInstance(recordStream)
        .withProcessInstanceKey(actual)
        .withRejectionType(RejectionType.NULL_VAL)
        .withoutBpmnElementType(BpmnElementType.PROCESS)
        .stream()
        .collect(
            Collectors.toMap(
                record ->
                    String.format(
                        "%s-%s",
                        record.getValue().getElementId(), record.getValue().getFlowScopeKey()),
                record -> record,
                (existing, replacement) -> replacement))
        .forEach(
            (key, record) -> {
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
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isWaitingExactlyAtElements(final String... elementIdsVarArg) {
    final List<String> elementIds = Arrays.asList(elementIdsVarArg);
    final Set<String> elementsInWaitState = getElementsInWaitState();
    final List<String> wrongfullyWaitingElementIds = new ArrayList<>();
    final List<String> wrongfullyNotWaitingElementIds = new ArrayList<>();

    StreamFilter.processInstance(recordStream)
        .withProcessInstanceKey(actual)
        .withRejectionType(RejectionType.NULL_VAL)
        .withoutBpmnElementType(BpmnElementType.PROCESS)
        .withIntent(ProcessInstanceIntent.ELEMENT_ACTIVATED)
        .stream()
        .map(Record::getValue)
        .map(ProcessInstanceRecordValue::getElementId)
        .distinct()
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
    softly
        .assertThat(wrongfullyWaitingElementIds.isEmpty())
        .withFailMessage(
            "Process with key %s is waiting at element(s) with id(s) %s",
            actual, String.join(", ", wrongfullyWaitingElementIds))
        .isTrue();
    softly
        .assertThat(wrongfullyNotWaitingElementIds.isEmpty())
        .withFailMessage(
            "Process with key %s is not waiting at element(s) with id(s) %s",
            actual, String.join(", ", wrongfullyNotWaitingElementIds))
        .isTrue();
    softly.assertAll();

    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently waiting to receive one or more
   * specified messages.
   *
   * @param messageNames Names of the messages
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isWaitingForMessages(final String... messageNames) {
    final Set<String> openMessageSubscriptions = getOpenMessageSubscriptions();
    assertThat(openMessageSubscriptions).containsAll(Arrays.asList(messageNames));
    return this;
  }

  /**
   * Verifies the expectation that the process instance is currently not waiting to receive one or
   * more specified messages.
   *
   * @param messageNames Names of the messages
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isNotWaitingForMessages(final String... messageNames) {
    final Set<String> openMessageSubscriptions = getOpenMessageSubscriptions();
    assertThat(openMessageSubscriptions).doesNotContainAnyElementsOf(Arrays.asList(messageNames));
    return this;
  }

  /**
   * Gets the currently open message subscription from the record stream source
   *
   * @return set containing the message names of the open message subscriptions
   */
  private Set<String> getOpenMessageSubscriptions() {
    final Set<String> openMessageSubscriptions = new HashSet<>();
    StreamFilter.processMessageSubscription(recordStream)
        .withProcessInstanceKey(actual)
        .withRejectionType(RejectionType.NULL_VAL)
        .stream()
        .collect(
            Collectors.toMap(
                record -> record.getValue().getElementInstanceKey(),
                record -> record,
                (existing, replacement) -> replacement))
        .forEach(
            (key, record) -> {
              if (record.getIntent().equals(ProcessMessageSubscriptionIntent.CREATING)
                  || record.getIntent().equals(ProcessMessageSubscriptionIntent.CREATED)) {
                openMessageSubscriptions.add(record.getValue().getMessageName());
              }
            });
    return openMessageSubscriptions;
  }

  /**
   * Verifies the expectation that a message with a given name has been correlated a given amount of
   * times.
   *
   * @param messageName The name of the message
   * @param times The expected amount of times the message is correlated
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasCorrelatedMessageByName(
      final String messageName, final int times) {
    assertThat(messageName).describedAs("Message name").isNotEmpty();
    assertThat(times).describedAs("Times").isGreaterThanOrEqualTo(0);

    final long actualTimes =
        StreamFilter.processMessageSubscription(recordStream)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
            .withMessageName(messageName)
            .withIntent(ProcessMessageSubscriptionIntent.CORRELATED)
            .stream()
            .count();

    assertThat(actualTimes)
        .withFailMessage(
            "Expected message with name '%s' to be correlated %d times, but was %d times",
            messageName, times, actualTimes)
        .isEqualTo(times);
    return this;
  }

  /**
   * Verifies the expectation that a message with a given correlation key has been correlated a
   * given amount of times.
   *
   * @param correlationKey The correlation key of the message
   * @param times The expected amount of times the message is correlated
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasCorrelatedMessageByCorrelationKey(
      final String correlationKey, final int times) {
    assertThat(correlationKey).describedAs("Correlation key").isNotEmpty();
    assertThat(times).describedAs("Times").isGreaterThanOrEqualTo(0);

    final long actualTimes =
        StreamFilter.processMessageSubscription(recordStream)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
            .withCorrelationKey(correlationKey)
            .withIntent(ProcessMessageSubscriptionIntent.CORRELATED)
            .stream()
            .count();

    assertThat(actualTimes)
        .withFailMessage(
            "Expected message with correlation key '%s' "
                + "to be correlated %d times, but was %d times",
            correlationKey, times, actualTimes)
        .isEqualTo(times);
    return this;
  }

  /**
   * @param name The name of the variable
   * @return this ${@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasVariable(final String name) {
    getVariableAssert().containsVariable(name);
    return this;
  }

  /**
   * Verifies the process instance has a variable with a specific value.
   *
   * @param name The name of the variable
   * @param value The value of the variable
   * @return this ${@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasVariableWithValue(final String name, final Object value) {
    getVariableAssert().hasVariableWithValue(name, value);

    return this;
  }

  /**
   * Returns a Map of variables that belong to this process instance. The values in that map are
   * JSON Strings
   *
   * @return map of variables
   */
  private Map<String, String> getProcessInstanceVariables() {
    return StreamFilter.variable(recordStream)
        .withProcessInstanceKey(actual)
        .withRejectionType(RejectionType.NULL_VAL)
        .stream()
        .sequential() // stream must be sequential for merge function to work
        .map(Record::getValue)
        .collect(
            Collectors.toMap(
                VariableRecordValue::getName,
                VariableRecordValue::getValue,
                (value1, value2) -> value2));
  }

  private VariablesMapAssert getVariableAssert() {
    return new VariablesMapAssert(getProcessInstanceVariables());
  }

  /**
   * Asserts whether any incidents were raised for this process instance (regardless of whether
   * these incidents are active or already resolved)
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasAnyIncidents() {
    final boolean incidentsWereRaised =
        getIncidentCreatedRecords().stream().findFirst().isPresent();

    assertThat(incidentsWereRaised)
        .withFailMessage("No incidents were raised for this process instance")
        .isTrue();
    return this;
  }

  /**
   * Asserts whether no incidents were raised for this process instance
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasNoIncidents() {
    final boolean incidentsWereRaised =
        getIncidentCreatedRecords().stream().findFirst().isPresent();

    assertThat(incidentsWereRaised)
        .withFailMessage("Incidents were raised for this process instance")
        .isFalse();
    return this;
  }

  /**
   * Extracts the latest incident
   *
   * @return {@link IncidentAssert} for the latest incident
   */
  public IncidentAssert extractingLatestIncident() {
    hasAnyIncidents();

    final List<Record<IncidentRecordValue>> incidentCreatedRecords =
        getIncidentCreatedRecords().stream().collect(Collectors.toList());

    final Record<IncidentRecordValue> latestIncidentRecord =
        incidentCreatedRecords.get(incidentCreatedRecords.size() - 1);

    return new IncidentAssert(latestIncidentRecord.getKey(), recordStream);
  }

  private IncidentRecordStreamFilter getIncidentCreatedRecords() {
    return StreamFilter.incident(recordStream)
        .withRejectionType(RejectionType.NULL_VAL)
        .withIntent(IncidentIntent.CREATED)
        .withProcessInstanceKey(actual);
  }

  /**
   * Extracts the latest called process. This will result in a failed assertion when no process has
   * been called. When a multi-instance loop is in your process, you might get an empty process, as
   * the multi-instance loop makes the processes it needs + one empty process.
   *
   * @return {@link ProcessInstanceAssert} for the called process
   */
  public ProcessInstanceAssert extractingLatestCalledProcess() {
    hasCalledProcess();

    final Record<ProcessInstanceRecordValue> latestCalledProcessRecord =
        getCalledProcessRecords().stream()
            .reduce((first, second) -> second)
            .orElseThrow(NoSuchElementException::new);

    return new ProcessInstanceAssert(latestCalledProcessRecord.getKey(), recordStream);
  }

  /**
   * Extracts the latest called process with a provided processId. This will result in a failed
   * assertion when the process has not been called. When a multi-instance loop is in your process,
   * you might get an empty process, as the multi-instance loop makes the processes it needs + one
   * empty process.
   *
   * @param processId The id of the process that should be called
   * @return {@link ProcessInstanceAssert} for the called process
   */
  public ProcessInstanceAssert extractingLatestCalledProcess(final String processId) {
    hasCalledProcess(processId);

    final Record<ProcessInstanceRecordValue> latestCalledProcessRecord =
        getCalledProcessRecords().withBpmnProcessId(processId).stream()
            .reduce((first, second) -> second)
            .orElseThrow(NoSuchElementException::new);

    return new ProcessInstanceAssert(latestCalledProcessRecord.getKey(), recordStream);
  }

  /**
   * Asserts whether this process has called another process
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasCalledProcess() {
    final boolean hasCalledProcess = getCalledProcessRecords().stream().findAny().isPresent();

    assertThat(hasCalledProcess)
        .withFailMessage("No process was called from this process")
        .isTrue();
    return this;
  }

  /**
   * Asserts whether this process has not called another process
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasNotCalledProcess() {
    final boolean hasCalledProcess = getCalledProcessRecords().stream().findAny().isPresent();

    assertThat(hasCalledProcess)
        .withFailMessage("A process was called from this process, distinct called processes are: %s",
            getCalledProcessRecords().stream().map(
                x -> x.getValue().getBpmnProcessId()).distinct().collect(Collectors.toList()).toString())
        .isFalse();
    return this;
  }

  /**
   * Asserts whether this process has called another specific process
   *
   * @param processId The id of the process that should have been called
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasCalledProcess(final String processId) {
    final boolean hasCalledProcess =
        getCalledProcessRecords().withBpmnProcessId(processId).stream().findAny().isPresent();

    assertThat(hasCalledProcess)
        .withFailMessage("No process with id `%s` was called from this process", processId)
        .isTrue();
    return this;
  }

  /**
   * Asserts whether this process has not called another specific process
   *
   * @param processId The id of the process that should have been called
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasNotCalledProcess(final String processId) {
    final boolean hasCalledProcess =
        getCalledProcessRecords().withBpmnProcessId(processId).stream().findAny().isPresent();

    assertThat(hasCalledProcess)
        .withFailMessage("A process with id `%s` was called from this process", processId)
        .isFalse();
    return this;
  }

  private ProcessInstanceRecordStreamFilter getCalledProcessRecords() {
    return StreamFilter.processInstance(recordStream).withParentProcessInstanceKey(actual);
  }
}
