/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.db;

import io.camunda.zeebe.db.DbKey;
import io.camunda.zeebe.db.impl.ZeebeDbConstants;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import org.agrona.ExpandableArrayBuffer;

/**
 * This class allows iterating over a subset of keys in the database. The subset is identified by a
 * common prefix for all keys in that subset. It also implements a recursion guard by checking that
 * iterations can only be nested up to a certain maximum depth.
 */
final class InMemoryDbColumnFamilyIterationContext {

  private final long columnFamilyPrefix;

  private final Queue<ExpandableArrayBuffer> prefixKeyBuffers =
      new ArrayDeque<>(List.of(new ExpandableArrayBuffer(), new ExpandableArrayBuffer()));

  InMemoryDbColumnFamilyIterationContext(final long columnFamilyPrefix) {
    this.columnFamilyPrefix = columnFamilyPrefix;
  }

  void withPrefixKey(final DbKey key, final Consumer<Bytes> prefixKeyConsumer) {
    if (prefixKeyBuffers.peek() == null) {
      throw new IllegalStateException(
          "Currently nested prefix iterations of this depth are not supported! This will cause unexpected behavior.");
    }

    final ExpandableArrayBuffer prefixKeyBuffer = prefixKeyBuffers.remove();
    try {
      prefixKeyBuffer.putLong(0, columnFamilyPrefix, ZeebeDbConstants.ZB_DB_BYTE_ORDER);
      key.write(prefixKeyBuffer, Long.BYTES);
      final int prefixLength = Long.BYTES + key.getLength();
      prefixKeyConsumer.accept(Bytes.fromByteArray(prefixKeyBuffer.byteArray(), prefixLength));
    } finally {
      prefixKeyBuffers.add(prefixKeyBuffer);
    }
  }
}
