package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MessageRecordStreamFilter {

  private final Stream<Record<MessageRecordValue>> stream;

  public MessageRecordStreamFilter(final Iterable<Record<MessageRecordValue>> messageRecords) {
    stream = StreamSupport.stream(messageRecords.spliterator(), false);
  }

  public MessageRecordStreamFilter(final Stream<Record<MessageRecordValue>> stream) {
    this.stream = stream;
  }

  public MessageRecordStreamFilter withKey(final long key) {
    return new MessageRecordStreamFilter(stream.filter(record -> record.getKey() == key));
  }

  public MessageRecordStreamFilter withMessageName(final String messageName) {
    return new MessageRecordStreamFilter(
        stream.filter(record -> record.getValue().getName().equals(messageName)));
  }

  public MessageRecordStreamFilter withCorrelationKey(final String correlationKey) {
    return new MessageRecordStreamFilter(
        stream.filter(record -> record.getValue().getCorrelationKey().equals(correlationKey)));
  }

  public MessageRecordStreamFilter withIntent(final MessageIntent intent) {
    return new MessageRecordStreamFilter(stream.filter(record -> record.getIntent() == intent));
  }

  public MessageRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new MessageRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public Stream<Record<MessageRecordValue>> stream() {
    return stream;
  }
}
