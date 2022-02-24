package io.camunda.zeebe.process.test.extension.testcontainer;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import java.util.ArrayList;

public class RecordStreamSourceImpl implements RecordStreamSource {

  private final ContainerizedEngine engine;
  private Iterable<Record<?>> records;

  public RecordStreamSourceImpl(final ContainerizedEngine engine) {
    this(engine, new ArrayList<>());
  }

  public RecordStreamSourceImpl(
      final ContainerizedEngine engine, final Iterable<Record<?>> records) {
    this.engine = engine;
    this.records = records;
  }

  @Override
  public Iterable<Record<?>> records() {
    updateWithNewRecords();
    return records;
  }

  private void updateWithNewRecords() {
    records = engine.getRecords();
  }
}
