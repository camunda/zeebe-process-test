/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.db;

import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.ZeebeDbFactory;
import io.camunda.zeebe.protocol.EnumValue;
import io.camunda.zeebe.util.micrometer.StatefulMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;

public class InMemoryDbFactory<ColumnFamilyType extends Enum<? extends EnumValue> & EnumValue>
    implements ZeebeDbFactory<ColumnFamilyType> {

  private final MeterRegistry meterRegistry;

  public InMemoryDbFactory() {
    this(new SimpleMeterRegistry());
  }

  public InMemoryDbFactory(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public ZeebeDb<ColumnFamilyType> createDb() {
    return createDb(null);
  }

  @Override
  public ZeebeDb<ColumnFamilyType> createDb(final File pathName) {
    final var stateful = new StatefulMeterRegistry(meterRegistry, Tags.empty());
    return new InMemoryDb<>(stateful);
  }

  @Override
  public ZeebeDb<ColumnFamilyType> openSnapshotOnlyDb(final File path) {
    throw new UnsupportedOperationException("Snapshots are not supported with in-memory databases");
  }
}
