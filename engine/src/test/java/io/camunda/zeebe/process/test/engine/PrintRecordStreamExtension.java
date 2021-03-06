/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.filters.RecordStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.util.ReflectionUtils;

class PrintRecordStreamExtension implements TestWatcher {

  @Override
  public void testFailed(final ExtensionContext context, final Throwable cause) {
    try {
      final Field zeebeEngineField = context.getRequiredTestClass().getDeclaredField("zeebeEngine");
      ReflectionUtils.makeAccessible(zeebeEngineField);
      final ZeebeTestEngine zeebeEngine =
          (ZeebeTestEngine) zeebeEngineField.get(context.getRequiredTestInstance());

      System.out.println("===== Test failed! Printing records from the stream:");
      RecordStream.of(zeebeEngine.getRecordStreamSource()).print(true);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
