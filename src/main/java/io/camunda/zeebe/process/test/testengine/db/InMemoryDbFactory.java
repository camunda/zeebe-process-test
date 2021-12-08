package io.camunda.zeebe.process.test.testengine.db;

import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.ZeebeDbFactory;
import java.io.File;

public class InMemoryDbFactory<ColumnFamilyTpe extends Enum<ColumnFamilyTpe>>
    implements ZeebeDbFactory<ColumnFamilyTpe> {

  public ZeebeDb<ColumnFamilyTpe> createDb() {
    return createDb(null);
  }

  @Override
  public ZeebeDb<ColumnFamilyTpe> createDb(final File pathName) {
    return new InMemoryDb<>();
  }
}
