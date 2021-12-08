package io.camunda.zeebe.process.test.testengine.db;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

final class InMemoryDbIterator {

  private final SortedMap<Bytes, Bytes> database;

  InMemoryDbIterator(final SortedMap<Bytes, Bytes> database) {
    this.database = database;
  }

  InMemoryDbIterator seek(final byte[] prefixedKey, final int prefixLength) {
    return new InMemoryDbIterator(database.tailMap(Bytes.fromByteArray(prefixedKey, prefixLength)));
  }

  Iterator<Map.Entry<Bytes, Bytes>> iterate() {
    return database.entrySet().iterator();
  }
}
