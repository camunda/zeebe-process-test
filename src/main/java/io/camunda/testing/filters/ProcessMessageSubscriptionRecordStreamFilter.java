package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProcessMessageSubscriptionRecordStreamFilter {

  private final Stream<Record<ProcessMessageSubscriptionRecordValue>> stream;

  public ProcessMessageSubscriptionRecordStreamFilter(
      final Iterable<Record<ProcessMessageSubscriptionRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public ProcessMessageSubscriptionRecordStreamFilter(
      final Stream<Record<ProcessMessageSubscriptionRecordValue>> stream) {
    this.stream = stream;
  }

  public ProcessMessageSubscriptionRecordStreamFilter withProcessInstanceKey(
      final long processInstanceKey) {
    return new ProcessMessageSubscriptionRecordStreamFilter(stream
        .filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public ProcessMessageSubscriptionRecordStreamFilter withMessageName(final String messageName) {
    return new ProcessMessageSubscriptionRecordStreamFilter(stream
        .filter(record -> record.getValue().getMessageName().equals(messageName)));
  }

  public Stream<Record<ProcessMessageSubscriptionRecordValue>> stream() {
    return stream;
  }
}
