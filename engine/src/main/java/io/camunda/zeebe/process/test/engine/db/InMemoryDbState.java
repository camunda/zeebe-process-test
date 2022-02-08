package io.camunda.zeebe.process.test.engine.db;

import io.camunda.zeebe.db.DbValue;

public interface InMemoryDbState {
  void put(FullyQualifiedKey fullyQualifiedKey, DbValue value);

  byte[] get(FullyQualifiedKey fullyQualifiedKey);

  void delete(FullyQualifiedKey fullyQualifiedKey);

  InMemoryDbIterator newIterator();
}
