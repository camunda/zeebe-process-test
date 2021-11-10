package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IncidentRecordStreamFilter {
  private final Stream<Record<IncidentRecordValue>> stream;

  public IncidentRecordStreamFilter(final Iterable<Record<IncidentRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public IncidentRecordStreamFilter(final Stream<Record<IncidentRecordValue>> stream) {
    this.stream = stream;
  }

  public IncidentRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new IncidentRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public IncidentRecordStreamFilter withProcessInstanceKey(final long processInstanceKey) {
    return new IncidentRecordStreamFilter(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public IncidentRecordStreamFilter withJobKey(final long jobKey) {
    return new IncidentRecordStreamFilter(
        stream.filter(record -> record.getValue().getJobKey() == jobKey));
  }

  public Stream<Record<IncidentRecordValue>> stream() {
    return stream;
  }
}
