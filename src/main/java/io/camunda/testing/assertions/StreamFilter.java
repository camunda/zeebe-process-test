package io.camunda.testing.assertions;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;

public class StreamFilter {

  public static ProcessInstanceRecordStreamFilter processInstance(
      Iterable<Record<ProcessInstanceRecordValue>> records) {
    return new ProcessInstanceRecordStreamFilter(records);
  }
}
