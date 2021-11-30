package io.camunda.zeebe.bpmnassert.testengine.db;

import io.camunda.zeebe.db.DbKey;
import io.camunda.zeebe.db.impl.ZeebeDbConstants;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Helper class that represents a fully qualified key. A fully qualified key is constructed from a
 * column family index and the key inside that column family
 */
final class FullyQualifiedKey {

  private final Bytes keyBytes;

  FullyQualifiedKey(final Enum columnFamily, final DbKey dbKey) {
    final ExpandableArrayBuffer keyBuffer = new ExpandableArrayBuffer();
    keyBuffer.putLong(0, columnFamily.ordinal(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);
    dbKey.write(keyBuffer, Long.BYTES);
    keyBytes = Bytes.fromExpandableArrayBuffer(keyBuffer);
  }

  Bytes getKeyBytes() {
    return keyBytes;
  }

  /**
   * Extract the key from a byte array containing a fully qualified key
   *
   * @param rawKeyBytes raw key as it is stored in the database, contains column family index as
   *     prefix
   * @return direct buffer with key part of
   */
  static DirectBuffer wrapKey(final byte[] rawKeyBytes) {
    final DirectBuffer keyViewBuffer = new UnsafeBuffer(0, 0);

    keyViewBuffer.wrap(rawKeyBytes, Long.BYTES, rawKeyBytes.length - Long.BYTES);

    return keyViewBuffer;
  }
}
