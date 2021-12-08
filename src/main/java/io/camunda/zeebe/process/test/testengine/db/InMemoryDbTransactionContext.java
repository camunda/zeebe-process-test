package io.camunda.zeebe.process.test.testengine.db;

import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.TransactionOperation;
import io.camunda.zeebe.db.ZeebeDbTransaction;
import java.util.TreeMap;

public class InMemoryDbTransactionContext implements TransactionContext {
  private final InMemoryDbTransaction transaction;

  public InMemoryDbTransactionContext(final TreeMap<Bytes, Bytes> database) {
    transaction = new InMemoryDbTransaction(database);
  }

  @Override
  public void runInTransaction(final TransactionOperation operations) {
    try {
      if (transaction.isInCurrentTransaction()) {
        operations.run();
      } else {
        runInNewTransaction(operations);
      }
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unexpected error occurred during zeebe db transaction operation.", e);
    }
  }

  private void runInNewTransaction(final TransactionOperation operations) throws Exception {
    try {
      transaction.resetTransaction();
      operations.run();
      transaction.commit();
    } catch (final Exception e) {
      transaction.rollback();
      throw e;
    }
  }

  @Override
  public ZeebeDbTransaction getCurrentTransaction() {
    if (!transaction.isInCurrentTransaction()) {
      transaction.resetTransaction();
    }
    return transaction;
  }
}
