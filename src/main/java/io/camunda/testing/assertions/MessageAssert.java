package io.camunda.testing.assertions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.camunda.testing.filters.StreamFilter;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
import org.assertj.core.api.AbstractAssert;
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
   * Verifies the expectation that a message has been deleted
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
}
