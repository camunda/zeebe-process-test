/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.agent;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

public class RecordStreamSourceWrapper {

  private final List<String> mappedRecords = new ArrayList<>();
  private final RecordStreamSource recordStreamSource;
  private volatile long lastEventPosition = -1L;

  public RecordStreamSourceWrapper(final RecordStreamSource recordStreamSource) {
    this.recordStreamSource = recordStreamSource;
  }

  public List<String> getMappedRecords() {
    synchronized (mappedRecords) {
      StreamSupport.stream(recordStreamSource.getRecords().spliterator(), false)
          .filter(record -> record.getPosition() > lastEventPosition)
          .forEach(
              record -> {
                mappedRecords.add(record.toJson());
                lastEventPosition = record.getPosition();
              });
    }

    return Collections.unmodifiableList(mappedRecords);
  }
}
