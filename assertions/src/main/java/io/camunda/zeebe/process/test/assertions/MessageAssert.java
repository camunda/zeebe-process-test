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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.MapAssert.assertThatMap;

import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.protocol.record.intent.MessageStartEventSubscriptionIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/** Assertions for {@link PublishMessageResponse} instances */
public class MessageAssert extends AbstractAssert<MessageAssert, PublishMessageResponse> {

  private final RecordStream recordStream;

  protected MessageAssert(final PublishMessageResponse actual, final RecordStream recordStream) {
    super(actual, MessageAssert.class);
    this.recordStream = recordStream;
  }

  /**
   * Verifies the expectation that a message has been correlated
   *
   * @return this {@link MessageAssert}
   */
  public MessageAssert hasBeenCorrelated() {
    final boolean isCorrelated =
        StreamFilter.processMessageSubscription(recordStream)
            .withMessageKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(ProcessMessageSubscriptionIntent.CORRELATED)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isCorrelated)
        .withFailMessage("Message with key %d was not correlated", actual.getMessageKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that a message has not been correlated
   *
   * @return this {@link MessageAssert}˚
   */
  public MessageAssert hasNotBeenCorrelated() {
    final Optional<Record<ProcessMessageSubscriptionRecordValue>> recordOptional =
        StreamFilter.processMessageSubscription(recordStream)
            .withMessageKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(ProcessMessageSubscriptionIntent.CORRELATED)
            .stream()
            .findFirst();

    assertThat(recordOptional.isPresent())
        .withFailMessage(
            "Message with key %d was correlated to process instance %d",
            actual.getMessageKey(),
            recordOptional
                .map(Record::getValue)
                .map(ProcessMessageSubscriptionRecordValue::getProcessInstanceKey)
                .orElse(-1L))
        .isFalse();

    return this;
  }

  /**
   * Verifies the expectation that a message start event has been correlated
   *
   * @return this {@link MessageAssert}
   */
  public MessageAssert hasCreatedProcessInstance() {
    final boolean isCorrelated =
        StreamFilter.messageStartEventSubscription(recordStream)
            .withMessageKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageStartEventSubscriptionIntent.CORRELATED)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isCorrelated)
        .withFailMessage(
            "Message with key %d did not lead to the creation of a process instance",
            actual.getMessageKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that a message start event has not been correlated
   *
   * @return this {@link MessageAssert}˚
   */
  public MessageAssert hasNotCreatedProcessInstance() {
    final Optional<Record<MessageStartEventSubscriptionRecordValue>> recordOptional =
        StreamFilter.messageStartEventSubscription(recordStream)
            .withMessageKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageStartEventSubscriptionIntent.CORRELATED)
            .stream()
            .findFirst();

    assertThat(recordOptional.isPresent())
        .withFailMessage(
            "Message with key %d was correlated to process instance %d",
            actual.getMessageKey(),
            recordOptional
                .map(Record::getValue)
                .map(MessageStartEventSubscriptionRecordValue::getProcessInstanceKey)
                .orElse(-1L))
        .isFalse();

    return this;
  }

  /**
   * Verifies the expectation that a message has expired
   *
   * @return this {@link MessageAssert}
   */
  public MessageAssert hasExpired() {
    final boolean isExpired =
        StreamFilter.message(recordStream)
            .withKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageIntent.EXPIRED)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isExpired)
        .withFailMessage("Message with key %d has not expired", actual.getMessageKey())
        .isTrue();

    return this;
  }

  /**
   * Verifies the expectation that a message has not expired
   *
   * @return this {@link MessageAssert}
   */
  public MessageAssert hasNotExpired() {
    final boolean isExpired =
        StreamFilter.message(recordStream)
            .withKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageIntent.EXPIRED)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isExpired)
        .withFailMessage("Message with key %d has expired", actual.getMessageKey())
        .isFalse();

    return this;
  }

  /**
   * Extracts the process instance for the given message
   *
   * @return Process instance assertions {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert extractingProcessInstance() {
    final List<Long> correlatedProcessInstances = getProcessInstanceKeysForCorrelatedMessage();
    correlatedProcessInstances.addAll(getProcessInstanceKeysForCorrelatedMessageStartEvent());

    Assertions.assertThat(correlatedProcessInstances)
        .withFailMessage(
            "Expected to find one correlated process instance for message key %d but found %d: %s",
            actual.getMessageKey(), correlatedProcessInstances.size(), correlatedProcessInstances)
        .hasSize(1);

    return new ProcessInstanceAssert(correlatedProcessInstances.get(0), recordStream);
  }

  /**
   * Gets the correlated process instance keys for process message subscriptions
   *
   * @return List of process instance keys
   */
  private List<Long> getProcessInstanceKeysForCorrelatedMessage() {
    return StreamFilter.processMessageSubscription(recordStream)
        .withMessageKey(actual.getMessageKey())
        .withRejectionType(RejectionType.NULL_VAL)
        .withIntent(ProcessMessageSubscriptionIntent.CORRELATED)
        .stream()
        .map(record -> record.getValue().getProcessInstanceKey())
        .collect(Collectors.toList());
  }

  /**
   * Gets the correlated process instance keys for message start event subscriptions
   *
   * @return List of process instance keys
   */
  private List<Long> getProcessInstanceKeysForCorrelatedMessageStartEvent() {
    return StreamFilter.messageStartEventSubscription(recordStream)
        .withMessageKey(actual.getMessageKey())
        .withRejectionType(RejectionType.NULL_VAL)
        .withIntent(MessageStartEventSubscriptionIntent.CORRELATED)
        .stream()
        .map(record -> record.getValue().getProcessInstanceKey())
        .collect(Collectors.toList());
  }

  private Map<String, Object> getVariables() {
    return StreamFilter.processMessageSubscription(recordStream)
        .withMessageKey(actual.getMessageKey())
        .withRejectionType(RejectionType.NULL_VAL)
        .stream()
        .map(record -> record.getValue().getVariables())
        .findFirst()
        .orElse(Collections.emptyMap());
  }

  /**
   * Verifies the message has the given variable.
   *
   * @param name The name of the variable
   * @return this ${@link MessageAssert}
   */
  public MessageAssert hasVariable(final String name) {
    final Map<String, Object> variables = getVariables();
    assertThatMap(variables)
        .withFailMessage(
            "Unable to find variable with name '%s'. Available variables are: %s",
            name, variables.keySet())
        .containsKeys(name);
    return this;
  }

  /**
   * Verifies the message has the given variable with the specified value.
   *
   * @param name The name of the variable
   * @param value The value of the variable
   * @return this ${@link MessageAssert}
   */
  public MessageAssert hasVariableWithValue(final String name, final Object value) {
    hasVariable(name);

    final Object actualValue = getVariables().get(name);
    assertThat(actualValue)
        .withFailMessage(
            "The variable '%s' does not have the expected value.%n"
                + "expected: %s%n"
                + "but was: %s",
            name, value, actualValue)
        .isEqualTo(value);
    return this;
  }
}
