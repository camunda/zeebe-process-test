package io.camunda.zeebe.bpmnassert.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JobRecordStreamFilter {

  private final Stream<Record<JobRecordValue>> stream;

  public JobRecordStreamFilter(final Iterable<Record<JobRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public JobRecordStreamFilter(final Stream<Record<JobRecordValue>> stream) {
    this.stream = stream;
  }

  public JobRecordStreamFilter withKey(final long key) {
    return new JobRecordStreamFilter(stream.filter(record -> record.getKey() == key));
  }

  public JobRecordStreamFilter withIntent(final JobIntent intent) {
    return new JobRecordStreamFilter(stream.filter(record -> record.getIntent() == intent));
  }

  public JobRecordStreamFilter withElementId(final String elementId) {
    return new JobRecordStreamFilter(
        stream.filter(record -> record.getValue().getElementId().equals(elementId)));
  }

  public Stream<Record<JobRecordValue>> stream() {
    return stream;
  }
}
