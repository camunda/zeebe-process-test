/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.db;

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
