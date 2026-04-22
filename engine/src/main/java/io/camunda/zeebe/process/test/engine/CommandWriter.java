/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.logstreams.log.LogAppendEntry;
import io.camunda.zeebe.logstreams.log.LogStreamWriter;
import io.camunda.zeebe.logstreams.log.WriteContext;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.impl.record.UnifiedRecordValue;

/**
 * This record is responsible for writing the commands to the {@link LogStreamWriter} in a
 * thread-safe way.
 */
record CommandWriter(LogStreamWriter writer) {

  void writeCommandWithKey(
      final Long key, final UnifiedRecordValue command, final RecordMetadata recordMetadata) {
    synchronized (writer) {
      writer.tryWrite(WriteContext.internal(), LogAppendEntry.of(key, recordMetadata, command));
    }
  }

  void writeCommandWithoutKey(
      final UnifiedRecordValue command, final RecordMetadata recordMetadata) {
    synchronized (writer) {
      writer.tryWrite(WriteContext.internal(), LogAppendEntry.of(recordMetadata, command));
    }
  }
}
