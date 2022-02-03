package io.camunda.zeebe.process.test.api;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.*;
import io.camunda.zeebe.protocol.record.value.deployment.Process;

public interface RecordStreamSource {

  /** @return an iterable of all {@link Record} */
  Iterable<Record<?>> records();

  /** @return an iterable of all {@link Record<ProcessInstanceRecordValue>} */
  Iterable<Record<ProcessInstanceRecordValue>> processInstanceRecords();

  /** @return an iterable of all {@link Record<JobRecordValue>} */
  Iterable<Record<JobRecordValue>> jobRecords();

  /** @return an iterable of all {@link Record<JobBatchRecordValue>} */
  Iterable<Record<JobBatchRecordValue>> jobBatchRecords();

  /** @return an iterable of all {@link Record<DeploymentRecordValue>} */
  Iterable<Record<DeploymentRecordValue>> deploymentRecords();

  /** @return an iterable of all {@link Record<Process>} */
  Iterable<Record<Process>> processRecords();

  /** @return an iterable of all {@link Record<VariableRecordValue>} */
  Iterable<Record<VariableRecordValue>> variableRecords();

  /** @return an iterable of all {@link Record<VariableDocumentRecordValue>} */
  Iterable<Record<VariableDocumentRecordValue>> variableDocumentRecords();

  /** @return an iterable of all {@link Record<IncidentRecordValue>} */
  Iterable<Record<IncidentRecordValue>> incidentRecords();

  /** @return an iterable of all {@link Record<TimerRecordValue>} */
  Iterable<Record<TimerRecordValue>> timerRecords();

  /** @return an iterable of all {@link Record<MessageRecordValue>} */
  Iterable<Record<MessageRecordValue>> messageRecords();

  /** @return an iterable of all {@link Record<MessageSubscriptionRecordValue>} */
  Iterable<Record<MessageSubscriptionRecordValue>> messageSubscriptionRecords();

  /** @return an iterable of all {@link Record<MessageStartEventSubscriptionRecordValue>} */
  Iterable<Record<MessageStartEventSubscriptionRecordValue>> messageStartEventSubscriptionRecords();

  /** @return an iterable of all {@link Record<ProcessMessageSubscriptionRecordValue>} */
  Iterable<Record<ProcessMessageSubscriptionRecordValue>> processMessageSubscriptionRecords();

  /**
   * Prints all records to the console
   *
   * @param compact enable compact logging
   */
  void print(final boolean compact);
}
