package io.camunda.zeebe.process.test.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessEventIntent;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProcessEventRecordStreamFilter {

  private final Stream<Record<ProcessEventRecordValue>> stream;

  public ProcessEventRecordStreamFilter(final Iterable<Record<?>> records) {
    stream =
        StreamSupport.stream(records.spliterator(), true)
            .filter(record -> record.getValueType() == ValueType.PROCESS_EVENT)
            .map(record -> (Record<ProcessEventRecordValue>) record);
  }

  public ProcessEventRecordStreamFilter(final Stream<Record<ProcessEventRecordValue>> stream) {
    this.stream = stream;
  }

  public ProcessEventRecordStreamFilter withIntent(final ProcessEventIntent intent) {
    return new ProcessEventRecordStreamFilter(
        stream.filter(record -> record.getIntent() == intent));
  }

  public ProcessEventRecordStreamFilter withTargetElementId(final String targetElementId) {
    return new ProcessEventRecordStreamFilter(
        stream.filter(record -> record.getValue().getTargetElementId().equals(targetElementId)));
  }

  public ProcessEventRecordStreamFilter withProcessDefinitionKey(final long processDefinitionKey) {
    return new ProcessEventRecordStreamFilter(
        stream.filter(
            record -> record.getValue().getProcessDefinitionKey() == processDefinitionKey));
  }

  public Stream<Record<ProcessEventRecordValue>> stream() {
    return stream;
  }
}
