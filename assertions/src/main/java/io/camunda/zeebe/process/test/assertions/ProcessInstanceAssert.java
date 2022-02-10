package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.impl.ZeebeObjectMapper;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.filters.IncidentRecordStreamFilter;
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
import java.util.*;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;

public class ProcessInstanceAssert extends AbstractAssert<ProcessInstanceAssert, Long> {

  private final RecordStreamSource recordStreamSource;

  public ProcessInstanceAssert(final long actual, final RecordStreamSource recordStreamSource) {
    super(actual, ProcessInstanceAssert.class);
    this.recordStreamSource = recordStreamSource;
  }

  /**
   * Verifies the expectation that the process instance is started. This will also be true when the
   * process has been completed or terminated.
   *
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isStarted() {
    final boolean isStarted =
        StreamFilter.processInstance(recordStreamSource)
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
        StreamFilter.processInstance(recordStreamSource)
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
    return StreamFilter.processInstance(recordStreamSource)
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
    return StreamFilter.processInstance(recordStreamSource)
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
        StreamFilter.processInstance(recordStreamSource)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
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
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert hasPassedElementInOrder(final String... elementIds) {
    final List<String> foundElementRecords =
        StreamFilter.processInstance(recordStreamSource)
            .withProcessInstanceKey(actual)
            .withRejectionType(RejectionType.NULL_VAL)
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
   * @param elementIds The ids of the elements
   * @return this {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert isWaitingAtElement(final String... elementIds) {
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
  public ProcessInstanceAssert isNotWaitingAtElement(final String... elementIds) {
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
    StreamFilter.processInstance(recordStreamSource)
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

    StreamFilter.processInstance(recordStreamSource)
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
  public ProcessInstanceAssert isWaitingForMessage(final String... messageNames) {
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
  public ProcessInstanceAssert isNotWaitingForMessage(final String... messageNames) {
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
    StreamFilter.processMessageSubscription(recordStreamSource)
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
        StreamFilter.processMessageSubscription(recordStreamSource)
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
        StreamFilter.processMessageSubscription(recordStreamSource)
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
    final Map<String, String> variables = getProcessInstanceVariables();
    return assertVariableInMapOfVariables(name, variables);
  }

  /**
   * Assert that the given variable name is a key in the given map of variables.
   *
   * <p>This assertion has been extracted from the method ${@link #hasVariable(String)} so that the
   * method ${@link #hasVariableWithValue(String, Object)} could reuse it without having to traverse
   * the record stream to collect the variables a second time.
   *
   * @param name The name of the variable
   * @param variables The map of variables
   * @return this ${@link ProcessInstanceAssert}
   */
  private ProcessInstanceAssert assertVariableInMapOfVariables(
      final String name, final Map<String, String> variables) {
    assertThat(variables)
        .withFailMessage(
            "Process with key %s does not contain variable with name `%s`. Available variables are: %s",
            actual, name, variables.keySet())
        .containsKey(name);
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
    final ZeebeObjectMapper mapper = new ZeebeObjectMapper();
    final String mappedValue = mapper.toJson(value);
    final Map<String, String> variables = getProcessInstanceVariables();

    assertVariableInMapOfVariables(name, variables);
    assertThat(variables)
        .withFailMessage(
            "The variable '%s' does not have the expected value. The value passed in"
                + " ('%s') is internally mapped to a JSON String that yields '%s'. However, the "
                + "actual value (as JSON String) is '%s'.",
            name, value, mappedValue, variables.get(name))
        .containsEntry(name, mappedValue);

    return this;
  }

  /**
   * Returns a Map of variables that belong to this process instance. The values in that map are
   * JSON Strings
   *
   * @return map of variables
   */
  private Map<String, String> getProcessInstanceVariables() {
    return StreamFilter.variable(recordStreamSource)
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
  public IncidentAssert extractLatestIncident() {
    hasAnyIncidents();

    final List<Record<IncidentRecordValue>> incidentCreatedRecords =
        getIncidentCreatedRecords().stream().collect(Collectors.toList());

    final Record<IncidentRecordValue> latestIncidentRecord =
        incidentCreatedRecords.get(incidentCreatedRecords.size() - 1);

    return new IncidentAssert(latestIncidentRecord.getKey(), recordStreamSource);
  }

  private IncidentRecordStreamFilter getIncidentCreatedRecords() {
    return StreamFilter.incident(recordStreamSource)
        .withRejectionType(RejectionType.NULL_VAL)
        .withIntent(IncidentIntent.CREATED)
        .withProcessInstanceKey(actual);
  }
}
