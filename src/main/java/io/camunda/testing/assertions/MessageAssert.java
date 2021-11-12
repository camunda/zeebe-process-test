package io.camunda.testing.assertions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.camunda.testing.filters.StreamFilter;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.protocol.record.intent.MessageStartEventSubscriptionIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.camunda.community.eze.RecordStreamSource;

public class MessageAssert extends AbstractAssert<MessageAssert, PublishMessageResponse> {

  private RecordStreamSource recordStreamSource;

  protected MessageAssert(
      final PublishMessageResponse actual, final RecordStreamSource recordStreamSource) {
    super(actual, MessageAssert.class);
    this.recordStreamSource = recordStreamSource;
  }

  /**
   * Verifies the expectation that a message has been correlated
   *
   * @return this {@link MessageAssert}
   */
  public MessageAssert hasBeenCorrelated() {
    final boolean isCorrelated =
        StreamFilter.processMessageSubscription(recordStreamSource)
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
   * Verifies the expectation that a message start event has been correlated
   *
   * @return this {@link MessageAssert}
   */
  public MessageAssert hasMessageStartEventBeenCorrelated() {
    final boolean isCorrelated =
        StreamFilter.messageStartEventSubscription(recordStreamSource)
            .withMessageKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageStartEventSubscriptionIntent.CORRELATED)
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
        StreamFilter.processMessageSubscription(recordStreamSource)
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
   * Verifies the expectation that a message start event has not been correlated
   *
   * @return this {@link MessageAssert}˚
   */
  public MessageAssert hasMessageStartEventNotBeenCorrelated() {
    final Optional<Record<MessageStartEventSubscriptionRecordValue>> recordOptional =
        StreamFilter.messageStartEventSubscription(recordStreamSource)
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
        StreamFilter.message(recordStreamSource)
            .withKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageIntent.EXPIRED)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isExpired)
        .withFailMessage("Message with key %d was not expired", actual.getMessageKey())
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
        StreamFilter.message(recordStreamSource)
            .withKey(actual.getMessageKey())
            .withRejectionType(RejectionType.NULL_VAL)
            .withIntent(MessageIntent.EXPIRED)
            .stream()
            .findFirst()
            .isPresent();

    assertThat(isExpired)
        .withFailMessage("Message with key %d was expired", actual.getMessageKey())
        .isFalse();

    return this;
  }

  /**
   * Extracts the process instance for the given message
   *
   * @param isMessageStartEvent is the message a start event
   * @return Process instance assertions {@link ProcessInstanceAssert}
   */
  public ProcessInstanceAssert extractingProcessInstance(final boolean isMessageStartEvent) {
    final List<Long> correlatedProcessInstances =
        isMessageStartEvent
            ? getProcessInstanceKeysForCorrelatedMessageStartEvent()
            : getProcessInstanceKeysForCorrelatedMessage();

    Assertions.assertThat(correlatedProcessInstances)
        .withFailMessage(
            "Expected to find one correlated process instance for message key %d but found %d: %s",
            actual.getMessageKey(), correlatedProcessInstances.size(), correlatedProcessInstances)
        .hasSize(1);

    return new ProcessInstanceAssert(correlatedProcessInstances.get(0), recordStreamSource);
  }

  /**
   * Gets the correlated process instance keys for process message subscriptions
   *
   * @return List of process instance keys
   */
  private List<Long> getProcessInstanceKeysForCorrelatedMessage() {
    return StreamFilter.processMessageSubscription(recordStreamSource)
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
    return StreamFilter.messageStartEventSubscription(recordStreamSource)
        .withMessageKey(actual.getMessageKey())
        .withRejectionType(RejectionType.NULL_VAL)
        .withIntent(MessageStartEventSubscriptionIntent.CORRELATED)
        .stream()
        .map(record -> record.getValue().getProcessInstanceKey())
        .collect(Collectors.toList());
  }
}
