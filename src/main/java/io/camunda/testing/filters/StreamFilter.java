package io.camunda.testing.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;

public class StreamFilter {

  public static ProcessInstanceRecordStreamFilter processInstance(
      final Iterable<Record<ProcessInstanceRecordValue>> records) {
    return new ProcessInstanceRecordStreamFilter(records);
  }

  public static ProcessMessageSubscriptionRecordStreamFilter processMessageSubscription(
      final Iterable<Record<ProcessMessageSubscriptionRecordValue>> records) {
    return new ProcessMessageSubscriptionRecordStreamFilter(records);
  }
}
