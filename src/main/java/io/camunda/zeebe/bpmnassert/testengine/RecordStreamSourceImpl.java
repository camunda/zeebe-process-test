package io.camunda.zeebe.bpmnassert.testengine;

import io.camunda.zeebe.engine.processing.streamprocessor.TypedEventRegistry;
import io.camunda.zeebe.logstreams.log.LogStreamReader;
import io.camunda.zeebe.logstreams.log.LoggedEvent;
import io.camunda.zeebe.protocol.impl.record.CopiedRecord;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.impl.record.UnifiedRecordValue;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableDocumentRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import io.camunda.zeebe.test.util.record.CompactRecordLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RecordStreamSourceImpl implements RecordStreamSource {

  private final LogStreamReader logStreamReader;
  private final int partitionId;
  private List<Record<?>> records = new ArrayList<>();
  private long lastPosition = -1L;

  public RecordStreamSourceImpl(final LogStreamReader logStreamReader, final int partitionId) {
    this.logStreamReader = logStreamReader;
    this.partitionId = partitionId;
  }

  @Override
  public Iterable<Record<?>> records() {
    updateWithNewRecords();
    return Collections.unmodifiableList(records);
  }

  <T extends RecordValue> Iterable<Record<T>> recordsOfValueType(final ValueType valueType) {
    final Stream<Record<T>> stream =
        (Stream)
            StreamSupport.stream(records().spliterator(), false)
                .filter((record) -> record.getValueType() == valueType);
    return stream::iterator;
  }

  @Override
  public Iterable<Record<ProcessInstanceRecordValue>> processInstanceRecords() {
    return recordsOfValueType(ValueType.PROCESS_INSTANCE);
  }

  @Override
  public Iterable<Record<JobRecordValue>> jobRecords() {
    return recordsOfValueType(ValueType.JOB);
  }

  @Override
  public Iterable<Record<JobBatchRecordValue>> jobBatchRecords() {
    return recordsOfValueType(ValueType.JOB_BATCH);
  }

  @Override
  public Iterable<Record<DeploymentRecordValue>> deploymentRecords() {
    return recordsOfValueType(ValueType.DEPLOYMENT);
  }

  @Override
  public Iterable<Record<Process>> processRecords() {
    return recordsOfValueType(ValueType.PROCESS);
  }

  @Override
  public Iterable<Record<VariableRecordValue>> variableRecords() {
    return recordsOfValueType(ValueType.VARIABLE);
  }

  @Override
  public Iterable<Record<VariableDocumentRecordValue>> variableDocumentRecords() {
    return recordsOfValueType(ValueType.VARIABLE_DOCUMENT);
  }

  @Override
  public Iterable<Record<IncidentRecordValue>> incidentRecords() {
    return recordsOfValueType(ValueType.INCIDENT);
  }

  @Override
  public Iterable<Record<TimerRecordValue>> timerRecords() {
    return recordsOfValueType(ValueType.TIMER);
  }

  @Override
  public Iterable<Record<MessageRecordValue>> messageRecords() {
    return recordsOfValueType(ValueType.MESSAGE);
  }

  @Override
  public Iterable<Record<MessageSubscriptionRecordValue>> messageSubscriptionRecords() {
    return recordsOfValueType(ValueType.MESSAGE_SUBSCRIPTION);
  }

  @Override
  public Iterable<Record<MessageStartEventSubscriptionRecordValue>>
      messageStartEventSubscriptionRecords() {
    return recordsOfValueType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION);
  }

  @Override
  public Iterable<Record<ProcessMessageSubscriptionRecordValue>>
      processMessageSubscriptionRecords() {
    return recordsOfValueType(ValueType.PROCESS_MESSAGE_SUBSCRIPTION);
  }

  @Override
  public void print(final boolean compact) {
    final List<Record<?>> recordsList = new ArrayList<>();
    records().forEach(recordsList::add);

    if (compact) {
      new CompactRecordLogger(recordsList).log();
    } else {
      System.out.println("===== records (count: ${count()}) =====");
      recordsList.forEach(record -> System.out.println(record.toJson()));
      System.out.println("---------------------------");
    }
  }

  private void updateWithNewRecords() {
    if (lastPosition < 0) {
      logStreamReader.seekToFirstEvent();
    } else {
      logStreamReader.seekToNextEvent(lastPosition);
    }

    while (logStreamReader.hasNext()) {
      final LoggedEvent event = logStreamReader.next();
      final CopiedRecord<UnifiedRecordValue> record = mapToRecord(event);
      records.add(record);
      lastPosition = event.getPosition();
    }
  }

  private CopiedRecord<UnifiedRecordValue> mapToRecord(final LoggedEvent event) {
    final RecordMetadata metadata = new RecordMetadata();
    event.readMetadata(metadata);

    final UnifiedRecordValue value;
    try {
      value =
          TypedEventRegistry.EVENT_REGISTRY
              .get(metadata.getValueType())
              .getDeclaredConstructor()
              .newInstance();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    event.readValue(value);

    return new CopiedRecord<>(
        value,
        metadata,
        event.getKey(),
        partitionId,
        event.getPosition(),
        event.getSourceEventPosition(),
        event.getTimestamp());
  }
}
