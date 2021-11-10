package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IncidentRecordStreamFiler {
  private final Stream<Record<IncidentRecordValue>> stream;

  public IncidentRecordStreamFiler(final Iterable<Record<IncidentRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public IncidentRecordStreamFiler(final Stream<Record<IncidentRecordValue>> stream) {
    this.stream = stream;
  }

  public IncidentRecordStreamFiler withRejectionType(final RejectionType rejectionType) {
    return new IncidentRecordStreamFiler(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public IncidentRecordStreamFiler withProcessInstanceKey(final long processInstanceKey) {
    return new IncidentRecordStreamFiler(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public IncidentRecordStreamFiler withJobKey(final long jobKey) {
    return new IncidentRecordStreamFiler(
        stream.filter(record -> record.getValue().getJobKey() == jobKey));
  }

  public Stream<Record<IncidentRecordValue>> stream() {
    return stream;
  }
}
