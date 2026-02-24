/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.db;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;

final class InMemoryDbIterator {

  private final NavigableMap<Bytes, Bytes> database;

  InMemoryDbIterator(final SortedMap<Bytes, Bytes> database) {
    if (database instanceof NavigableMap) {
      this.database = (NavigableMap<Bytes, Bytes>) database;
    } else {
      this.database = new java.util.TreeMap<>(database);
    }
  }

  InMemoryDbIterator seek(final byte[] prefixedKey, final int prefixLength) {
    return new InMemoryDbIterator(
        database.tailMap(Bytes.fromByteArray(prefixedKey, prefixLength), true));
  }

  InMemoryDbIterator seekReverse(final byte[] prefixedKey, final int prefixLength) {
    final Bytes seekKey = Bytes.fromByteArray(prefixedKey, prefixLength);
    return new InMemoryDbIterator(database.headMap(seekKey, true));
  }

  Iterator<Map.Entry<Bytes, Bytes>> iterate() {
    return database.entrySet().iterator();
  }

  Iterator<Map.Entry<Bytes, Bytes>> iterateReverse() {
    return database.descendingMap().entrySet().iterator();
  }
}
