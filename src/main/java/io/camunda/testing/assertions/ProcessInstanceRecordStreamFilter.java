package io.camunda.testing.assertions;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProcessInstanceRecordStreamFilter {

  private Stream<Record<ProcessInstanceRecordValue>> stream;

  public ProcessInstanceRecordStreamFilter(
      final Iterable<Record<ProcessInstanceRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public ProcessInstanceRecordStreamFilter withProcessInstanceKey(final long processInstanceKey) {
    stream =
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey);
    return this;
  }

  public ProcessInstanceRecordStreamFilter withBpmnElementType(
      final BpmnElementType bpmnElementType) {
    stream = stream.filter(record -> record.getValue().getBpmnElementType() == bpmnElementType);
    return this;
  }

  public ProcessInstanceRecordStreamFilter withIntent(final ProcessInstanceIntent intent) {
    stream = stream.filter(record -> record.getIntent() == intent);
    return this;
  }

  public ProcessInstanceRecordStreamFilter withElementId(final String elementId) {
    stream = stream.filter(record -> record.getValue().getElementId().equals(elementId));
    return this;
  }

  public Stream<Record<ProcessInstanceRecordValue>> stream() {
    return stream;
  }
}
