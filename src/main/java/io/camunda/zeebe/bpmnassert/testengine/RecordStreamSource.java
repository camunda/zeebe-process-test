package io.camunda.zeebe.bpmnassert.testengine;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.*;
import io.camunda.zeebe.protocol.record.value.deployment.Process;

public interface RecordStreamSource {

  Iterable<Record<?>> records();

  Iterable<Record<ProcessInstanceRecordValue>> processInstanceRecords();

  Iterable<Record<JobRecordValue>> jobRecords();

  Iterable<Record<JobBatchRecordValue>> jobBatchRecords();

  Iterable<Record<DeploymentRecordValue>> deploymentRecords();

  Iterable<Record<Process>> processRecords();

  Iterable<Record<VariableRecordValue>> variableRecords();

  Iterable<Record<VariableDocumentRecordValue>> variableDocumentRecords();

  Iterable<Record<IncidentRecordValue>> incidentRecords();

  Iterable<Record<TimerRecordValue>> timerRecords();

  Iterable<Record<MessageRecordValue>> messageRecords();

  Iterable<Record<MessageSubscriptionRecordValue>> messageSubscriptionRecords();

  Iterable<Record<MessageStartEventSubscriptionRecordValue>> messageStartEventSubscriptionRecords();

  Iterable<Record<ProcessMessageSubscriptionRecordValue>> processMessageSubscriptionRecords();

  void print(final boolean compact);
}
