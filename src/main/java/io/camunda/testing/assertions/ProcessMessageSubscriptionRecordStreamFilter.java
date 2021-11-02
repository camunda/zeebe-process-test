package io.camunda.testing.assertions;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProcessMessageSubscriptionRecordStreamFilter {

  private Stream<Record<ProcessMessageSubscriptionRecordValue>> stream;

  public ProcessMessageSubscriptionRecordStreamFilter(
      final Iterable<Record<ProcessMessageSubscriptionRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public ProcessMessageSubscriptionRecordStreamFilter withProcessInstanceKey(
      final long processInstanceKey) {
    stream =
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey);
    return this;
  }

  public ProcessMessageSubscriptionRecordStreamFilter withMessageName(final String messageName) {
    stream = stream.filter(record -> record.getValue().getMessageName().equals(messageName));
    return this;
  }

  public Stream<Record<ProcessMessageSubscriptionRecordValue>> stream() {
    return stream;
  }
}
