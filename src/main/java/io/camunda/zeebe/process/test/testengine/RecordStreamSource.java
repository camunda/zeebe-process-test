package io.camunda.zeebe.process.test.testengine;

import io.camunda.zeebe.logstreams.log.LogStream;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.*;
import io.camunda.zeebe.protocol.record.value.deployment.Process;

public interface RecordStreamSource {

  /**
   * @return an iterable of all {@link Record} on the {@link LogStream}
   */
  Iterable<Record<?>> records();

  /**
   * @return an iterable of all {@link Record<ProcessInstanceRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<ProcessInstanceRecordValue>> processInstanceRecords();

  /**
   * @return an iterable of all {@link Record<JobRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<JobRecordValue>> jobRecords();

  /**
   * @return an iterable of all {@link Record<JobBatchRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<JobBatchRecordValue>> jobBatchRecords();

  /**
   * @return an iterable of all {@link Record<DeploymentRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<DeploymentRecordValue>> deploymentRecords();

  /**
   * @return an iterable of all {@link Record<Process>} on the {@link LogStream}
   */
  Iterable<Record<Process>> processRecords();

  /**
   * @return an iterable of all {@link Record<VariableRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<VariableRecordValue>> variableRecords();

  /**
   * @return an iterable of all {@link Record<VariableDocumentRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<VariableDocumentRecordValue>> variableDocumentRecords();

  /**
   * @return an iterable of all {@link Record<IncidentRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<IncidentRecordValue>> incidentRecords();

  /**
   * @return an iterable of all {@link Record<TimerRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<TimerRecordValue>> timerRecords();

  /**
   * @return an iterable of all {@link Record<MessageRecordValue>} on the {@link LogStream}
   */
  Iterable<Record<MessageRecordValue>> messageRecords();

  /**
   * @return an iterable of all {@link Record<MessageSubscriptionRecordValue>} on the
   * {@link LogStream}
   */
  Iterable<Record<MessageSubscriptionRecordValue>> messageSubscriptionRecords();

  /**
   * @return an iterable of all {@link Record<MessageStartEventSubscriptionRecordValue>} on the
   * {@link LogStream}
   */
  Iterable<Record<MessageStartEventSubscriptionRecordValue>> messageStartEventSubscriptionRecords();

  /**
   * @return an iterable of all {@link Record<ProcessMessageSubscriptionRecordValue>} on the
   * {@link LogStream}
   */
  Iterable<Record<ProcessMessageSubscriptionRecordValue>> processMessageSubscriptionRecords();

  /**
   * Prints all records to the console
   *
   * @param compact enable compact logging
   */
  void print(final boolean compact);
}
