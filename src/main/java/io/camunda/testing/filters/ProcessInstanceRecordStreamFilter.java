package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProcessInstanceRecordStreamFilter {

  private final Stream<Record<ProcessInstanceRecordValue>> stream;

  public ProcessInstanceRecordStreamFilter(
      final Iterable<Record<ProcessInstanceRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public ProcessInstanceRecordStreamFilter(
      final Stream<Record<ProcessInstanceRecordValue>> stream) {
    this.stream = stream;
  }

  public ProcessInstanceRecordStreamFilter withProcessInstanceKey(final long processInstanceKey) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public ProcessInstanceRecordStreamFilter withBpmnElementType(
      final BpmnElementType bpmnElementType) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getValue().getBpmnElementType() == bpmnElementType));
  }

  public ProcessInstanceRecordStreamFilter withoutBpmnElementType(
      final BpmnElementType bpmnElementType) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getValue().getBpmnElementType() != bpmnElementType));
  }

  public ProcessInstanceRecordStreamFilter withIntent(final ProcessInstanceIntent intent) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getIntent() == intent));
  }

  public ProcessInstanceRecordStreamFilter withElementId(final String elementId) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getValue().getElementId().equals(elementId)));
  }

  public ProcessInstanceRecordStreamFilter withElementIdIn(final String... elementIds) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(
            record -> Arrays.asList(elementIds).contains(record.getValue().getElementId())));
  }

  public ProcessInstanceRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public ProcessInstanceRecordStreamFilter withParentProcessInstanceKey(
      final long parentProcessInstanceKey) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(
            record -> record.getValue().getParentProcessInstanceKey() == parentProcessInstanceKey));
  }

  public ProcessInstanceRecordStreamFilter withBpmnProcessId(final String bpmnProcessId) {
    return new ProcessInstanceRecordStreamFilter(
        stream.filter(record -> record.getValue().getBpmnProcessId().equals(bpmnProcessId)));
  }

  public Stream<Record<ProcessInstanceRecordValue>> stream() {
    return stream;
  }
}
