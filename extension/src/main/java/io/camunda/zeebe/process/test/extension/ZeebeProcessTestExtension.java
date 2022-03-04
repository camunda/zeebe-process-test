/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.extension;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.engine.EngineFactory;
import io.camunda.zeebe.process.test.filters.RecordStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This extension is used by the {@link ZeebeProcessTest} annotation. It is responsible for managing
 * the lifecycle of the test engine.
 */
public class ZeebeProcessTestExtension
    implements BeforeEachCallback, AfterEachCallback, TestWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(ZeebeProcessTestExtension.class);
  private static final String KEY_ZEEBE_CLIENT = "ZEEBE_CLIENT";
  private static final String KEY_ZEEBE_ENGINE = "ZEEBE_ENGINE";

  /**
   * Before each test a new test engine gets created and started. A client to communicate with the
   * engine will also be created. Together with a {@link RecordStream} these will be injected in the
   * fields of the test class, if they are available.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void beforeEach(final ExtensionContext extensionContext) {
    final InMemoryEngine engine = EngineFactory.create();
    engine.start();
    final ZeebeClient client = engine.createClient();
    final RecordStream recordStream = RecordStream.of(engine.getRecordStreamSource());

    try {
      injectFields(extensionContext, engine, client, recordStream);
    } catch (final Exception ex) {
      client.close();
      engine.stop();
      throw ex;
    }

    BpmnAssert.initRecordStream(recordStream);
    getStore(extensionContext).put(KEY_ZEEBE_CLIENT, client);
    getStore(extensionContext).put(KEY_ZEEBE_ENGINE, engine);
  }

  /**
   * After each test the test engine en client will be closed. The {@link RecordStream} will get
   * reset.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void afterEach(final ExtensionContext extensionContext) {
    BpmnAssert.resetRecordStream();

    final Object clientContent = getStore(extensionContext).get(KEY_ZEEBE_CLIENT);
    final ZeebeClient client = (ZeebeClient) clientContent;
    client.close();

    final Object engineContent = getStore(extensionContext).get(KEY_ZEEBE_ENGINE);
    final InMemoryEngine engine = (InMemoryEngine) engineContent;
    engine.stop();
  }

  /**
   * Upon test failure an overview of occurred events and incidents will be logged.
   *
   * @param extensionContext jUnit5 extension context
   * @param cause the throwable that caused the test failure
   */
  @Override
  public void testFailed(final ExtensionContext extensionContext, final Throwable cause) {
    final Object engineContent = getStore(extensionContext).get(KEY_ZEEBE_ENGINE);
    final InMemoryEngine engine = (InMemoryEngine) engineContent;

    LOG.error("===== Test failed!");
    RecordStream.of(engine.getRecordStreamSource()).print(true);
  }

  private void injectFields(final ExtensionContext extensionContext, final Object... objects) {
    final Class<?> requiredTestClass = extensionContext.getRequiredTestClass();
    final Field[] declaredFields = requiredTestClass.getDeclaredFields();
    for (Object object : objects) {
      final Optional<Field> field = getField(declaredFields, object);
      field.ifPresent(value -> injectField(extensionContext, value, object));
    }
  }

  private Optional<Field> getField(final Field[] declaredFields, final Object object) {
    final List<Field> fields =
        Arrays.stream(declaredFields)
            .filter(field -> field.getType().isInstance(object))
            .collect(Collectors.toList());

    if (fields.size() > 1) {
      throw new IllegalStateException(
          String.format(
              "Expected at most one field of type %s, but "
                  + "found %s. Please make sure at most one field of type %s has been declared in the"
                  + " test class.",
              object.getClass().getSimpleName(), fields.size(), object.getClass().getSimpleName()));
    }
    return fields.size() == 0 ? Optional.empty() : Optional.of(fields.get(0));
  }

  private void injectField(
      final ExtensionContext extensionContext, final Field field, final Object object) {
    try {
      ReflectionUtils.makeAccessible(field);
      field.set(extensionContext.getRequiredTestInstance(), object);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private ExtensionContext.Store getStore(final ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getUniqueId()));
  }
}
