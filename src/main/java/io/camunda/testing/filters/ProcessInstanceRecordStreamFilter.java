package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.Arrays;
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

  public ProcessInstanceRecordStreamFilter withoutBpmnElementType(
      final BpmnElementType bpmnElementType) {
    stream = stream.filter(record -> record.getValue().getBpmnElementType() != bpmnElementType);
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

  public ProcessInstanceRecordStreamFilter withElementIdIn(final String... elementIds) {
    stream =
        stream.filter(
            record -> Arrays.asList(elementIds).contains(record.getValue().getElementId()));
    return this;
  }

  public Stream<Record<ProcessInstanceRecordValue>> stream() {
    return stream;
  }
}
