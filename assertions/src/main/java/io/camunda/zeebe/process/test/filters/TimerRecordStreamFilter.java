package io.camunda.zeebe.process.test.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.TimerIntent;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TimerRecordStreamFilter {

  private final Stream<Record<TimerRecordValue>> stream;

  public TimerRecordStreamFilter(final Iterable<Record<TimerRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public TimerRecordStreamFilter(final Stream<Record<TimerRecordValue>> stream) {
    this.stream = stream;
  }

  public TimerRecordStreamFilter withIntent(final TimerIntent intent) {
    return new TimerRecordStreamFilter(stream.filter(record -> record.getIntent() == intent));
  }

  public Stream<Record<TimerRecordValue>> stream() {
    return stream;
  }
}
