package io.camunda.zeebe.bpmnassert.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageStartEventSubscriptionIntent;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MessageStartEventSubscriptionStreamFilter {

  private final Stream<Record<MessageStartEventSubscriptionRecordValue>> stream;

  public MessageStartEventSubscriptionStreamFilter(
      final Iterable<Record<MessageStartEventSubscriptionRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public MessageStartEventSubscriptionStreamFilter(
      final Stream<Record<MessageStartEventSubscriptionRecordValue>> stream) {
    this.stream = stream;
  }

  public MessageStartEventSubscriptionStreamFilter withMessageKey(final long messageKey) {
    return new MessageStartEventSubscriptionStreamFilter(
        stream.filter(record -> record.getValue().getMessageKey() == messageKey));
  }

  public MessageStartEventSubscriptionStreamFilter withRejectionType(
      final RejectionType rejectionType) {
    return new MessageStartEventSubscriptionStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public MessageStartEventSubscriptionStreamFilter withIntent(
      final MessageStartEventSubscriptionIntent intent) {
    return new MessageStartEventSubscriptionStreamFilter(
        stream.filter(record -> record.getIntent() == intent));
  }

  public Stream<Record<MessageStartEventSubscriptionRecordValue>> stream() {
    return stream;
  }
}
