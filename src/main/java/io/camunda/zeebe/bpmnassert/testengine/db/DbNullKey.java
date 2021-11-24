package io.camunda.zeebe.bpmnassert.testengine.db;

import io.camunda.zeebe.db.DbKey;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/** This class is only used internally to search the database with the same column family prefix */
final class DbNullKey implements DbKey {

  static final DbNullKey INSTANCE = new DbNullKey();

  private DbNullKey() {}

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    // do nothing
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    // do nothing
  }
}
