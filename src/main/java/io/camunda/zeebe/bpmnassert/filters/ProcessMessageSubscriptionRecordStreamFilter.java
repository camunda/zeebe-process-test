package io.camunda.zeebe.bpmnassert.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.ProcessMessageSubscriptionIntent;
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
    return new ProcessMessageSubscriptionRecordStreamFilter(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public ProcessMessageSubscriptionRecordStreamFilter withMessageName(final String messageName) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        stream.filter(record -> record.getValue().getMessageName().equals(messageName)));
  }

  public ProcessMessageSubscriptionRecordStreamFilter withCorrelationKey(
      final String correlationKey) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        stream.filter(record -> record.getValue().getCorrelationKey().equals(correlationKey)));
  }

  public ProcessMessageSubscriptionRecordStreamFilter withRejectionType(
      final RejectionType rejectionType) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public ProcessMessageSubscriptionRecordStreamFilter withIntent(
      final ProcessMessageSubscriptionIntent intent) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        stream.filter(record -> record.getIntent() == intent));
  }

  public ProcessMessageSubscriptionRecordStreamFilter withMessageKey(final long messageKey) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        stream.filter(record -> record.getValue().getMessageKey() == messageKey));
  }

  public Stream<Record<ProcessMessageSubscriptionRecordValue>> stream() {
    return stream;
  }
}
