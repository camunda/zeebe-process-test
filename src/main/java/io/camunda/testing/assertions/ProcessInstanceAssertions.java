package io.camunda.testing.assertions;

import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import java.util.Optional;
import org.assertj.core.api.AbstractAssert;
import org.camunda.community.eze.RecordStreamSource;

public class ProcessInstanceAssertions extends
    AbstractAssert<ProcessInstanceAssertions, ProcessInstanceEvent> {

  private RecordStreamSource recordStreamSource;

  public ProcessInstanceAssertions(final ProcessInstanceEvent actual,
      final RecordStreamSource recordStreamSource) {
    super(actual, ProcessInstanceAssertions.class);
    this.recordStreamSource = recordStreamSource;
  }

  public static ProcessInstanceAssertions assertThat(final ProcessInstanceEvent actual, final RecordStreamSource recordStreamSource) {
    return new ProcessInstanceAssertions(actual, recordStreamSource);
  }

  public ProcessInstanceAssertions isStarted() {
    final Optional<Record<ProcessInstanceRecordValue>> processInstanceRecord =
        StreamFilter.processInstance(recordStreamSource.processInstanceRecords())
            .withProcessInstanceKey(actual.getProcessInstanceKey())
            .withBpmnElementType(BpmnElementType.PROCESS)
            .stream()
            .findFirst();

    if (!processInstanceRecord.isPresent()) {
      failWithMessage("Process with key %s was not started", actual.getProcessInstanceKey());
    }

    return this;
  }
}
