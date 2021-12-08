package io.camunda.zeebe.process.test.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class VariableRecordStreamFilter {

  private Stream<Record<VariableRecordValue>> stream;

  public VariableRecordStreamFilter(final Iterable<Record<VariableRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public VariableRecordStreamFilter(final Stream<Record<VariableRecordValue>> stream) {
    this.stream = stream;
  }

  public VariableRecordStreamFilter withProcessInstanceKey(final long processInstanceKey) {
    return new VariableRecordStreamFilter(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public VariableRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new VariableRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public Stream<Record<VariableRecordValue>> stream() {
    return stream;
  }
}
