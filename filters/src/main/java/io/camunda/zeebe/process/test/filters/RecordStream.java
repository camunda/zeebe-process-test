/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.process.test.filters;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.filters.logger.IncidentLogger;
import io.camunda.zeebe.process.test.filters.logger.RecordStreamLogger;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableDocumentRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordStream {

  private static final Logger LOG = LoggerFactory.getLogger("io.camunda.zeebe.process.test");

  private final RecordStreamSource recordStreamSource;

  private RecordStream(final RecordStreamSource recordStreamSource) {
    this.recordStreamSource = recordStreamSource;
  }

  public static RecordStream of(final RecordStreamSource recordStreamSource) {
    return new RecordStream(recordStreamSource);
  }

  public Iterable<Record<?>> records() {
    return recordStreamSource.getRecords();
  }

  <T extends RecordValue> Iterable<Record<T>> recordsOfValueType(final ValueType valueType) {
    final Stream<Record<T>> stream =
        (Stream)
            StreamSupport.stream(recordStreamSource.getRecords().spliterator(), false)
                .filter((record) -> record.getValueType() == valueType);
    return stream::iterator;
  }

  public Iterable<Record<ProcessInstanceRecordValue>> processInstanceRecords() {
    return recordsOfValueType(ValueType.PROCESS_INSTANCE);
  }

  public Iterable<Record<JobRecordValue>> jobRecords() {
    return recordsOfValueType(ValueType.JOB);
  }

  public Iterable<Record<JobBatchRecordValue>> jobBatchRecords() {
    return recordsOfValueType(ValueType.JOB_BATCH);
  }

  public Iterable<Record<DeploymentRecordValue>> deploymentRecords() {
    return recordsOfValueType(ValueType.DEPLOYMENT);
  }

  public Iterable<Record<Process>> processRecords() {
    return recordsOfValueType(ValueType.PROCESS);
  }

  public Iterable<Record<VariableRecordValue>> variableRecords() {
    return recordsOfValueType(ValueType.VARIABLE);
  }

  public Iterable<Record<VariableDocumentRecordValue>> variableDocumentRecords() {
    return recordsOfValueType(ValueType.VARIABLE_DOCUMENT);
  }

  public Iterable<Record<IncidentRecordValue>> incidentRecords() {
    return recordsOfValueType(ValueType.INCIDENT);
  }

  public Iterable<Record<TimerRecordValue>> timerRecords() {
    return recordsOfValueType(ValueType.TIMER);
  }

  public Iterable<Record<MessageRecordValue>> messageRecords() {
    return recordsOfValueType(ValueType.MESSAGE);
  }

  public Iterable<Record<MessageBatchRecordValue>> messageBatchRecords() {
    return recordsOfValueType(ValueType.MESSAGE_BATCH);
  }

  public Iterable<Record<MessageSubscriptionRecordValue>> messageSubscriptionRecords() {
    return recordsOfValueType(ValueType.MESSAGE_SUBSCRIPTION);
  }

  public Iterable<Record<MessageStartEventSubscriptionRecordValue>>
      messageStartEventSubscriptionRecords() {
    return recordsOfValueType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION);
  }

  public Iterable<Record<ProcessMessageSubscriptionRecordValue>>
      processMessageSubscriptionRecords() {
    return recordsOfValueType(ValueType.PROCESS_MESSAGE_SUBSCRIPTION);
  }

  public void print(final boolean compact) {
    if (compact) {
      new IncidentLogger(recordStreamSource).log();
      new RecordStreamLogger(recordStreamSource).log();
    } else {
      LOG.info("===== records (count: ${count()}) =====");
      recordStreamSource.getRecords().forEach(record -> LOG.info(record.toJson()));
      LOG.info("---------------------------");
    }
  }
}
