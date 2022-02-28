package io.camunda.zeebe.process.test.extension.testcontainer;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import java.util.ArrayList;

/**
 * The source for record processed by the test engine. This class is responsible for getting the
 * records from the test engine and storing them locally.
 */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<Record<?>> records() {
    updateWithNewRecords();
    return records;
  }

  private void updateWithNewRecords() {
    records = engine.getRecords();
  }
}
