package io.camunda.zeebe.process.test.testengine.db;

import io.camunda.zeebe.db.DbValue;
import io.camunda.zeebe.db.TransactionOperation;
import io.camunda.zeebe.db.ZeebeDbTransaction;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * In memory implementation of {@code ZeebeDbTransaction}
 *
 * <p>This implementation maintains a transaction cache and a list of deleted keys to capture the
 * changes performed during the transaction. Upon commit, the changes of this transaction are
 * written into the database, This transaction reads with "read committed" isolation level.
 *
 * <p>There is no locking mechanism between transactions.
 */
final class InMemoryDbTransaction implements ZeebeDbTransaction, InMemoryDbState {

  private final TreeMap<Bytes, Bytes> database;
  private final TreeMap<Bytes, Bytes> transactionCache = new TreeMap<>();
  private final HashSet<Bytes> deletedKeys = new HashSet<>();
  private boolean inCurrentTransaction = false;

  InMemoryDbTransaction(final TreeMap<Bytes, Bytes> database) {
    this.database = database;
  }

  void resetTransaction() {
    rollback();
    inCurrentTransaction = true;
  }

  public boolean isInCurrentTransaction() {
    return inCurrentTransaction;
  }

  @Override
  public void run(final TransactionOperation operations) throws Exception {
    operations.run();
  }

  @Override
  public void commit() {
    inCurrentTransaction = false;
    database.putAll(transactionCache);
    deletedKeys.forEach(database::remove);
    deletedKeys.clear();
    transactionCache.clear();
  }

  @Override
  public void rollback() {
    inCurrentTransaction = false;
    deletedKeys.clear();
    transactionCache.clear();
  }

  @Override
  public void put(final FullyQualifiedKey fullyQualifiedKey, final DbValue value) {
    deletedKeys.remove(fullyQualifiedKey.getKeyBytes());
    transactionCache.put(fullyQualifiedKey.getKeyBytes(), Bytes.fromDbValue(value));
  }

  @Override
  public byte[] get(final FullyQualifiedKey fullyQualifiedKey) {
    final Bytes key = fullyQualifiedKey.getKeyBytes();

    final Bytes valueInCache = transactionCache.get(key);

    if (valueInCache != null) {
      return valueInCache.toBytes();
    }

    final Bytes valueInDatabase = database.get(key);

    if (valueInDatabase != null) {
      return valueInDatabase.toBytes();
    } else {
      return null;
    }
  }

  @Override
  public void delete(final FullyQualifiedKey fullyQualifiedKey) {
    final Bytes keyBytes = fullyQualifiedKey.getKeyBytes();
    transactionCache.remove(keyBytes);
    deletedKeys.add(keyBytes);
  }

  @Override
  public InMemoryDbIterator newIterator() {
    final TreeMap<Bytes, Bytes> snapshot = new TreeMap<>();
    snapshot.putAll(database);
    snapshot.putAll(transactionCache);

    return new InMemoryDbIterator(snapshot);
  }
}
