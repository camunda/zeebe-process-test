/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.process.test.extension.testcontainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
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
    implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, TestWatcher {

  private static final Logger LOG = LoggerFactory.getLogger(ZeebeProcessTestExtension.class);
  private static final String KEY_ZEEBE_CLIENT = "ZEEBE_CLIENT";
  private static final String KEY_ZEEBE_ENGINE = "ZEEBE_ENGINE";

  private final ObjectMapper objectMapperInstance = new ObjectMapper();

  /**
   * A new testcontainer container gets created. After this a {@link ContainerizedEngine} is
   * created, which allows communicating with the testcontainer.
   *
   * <p>Even though this method is called before each test class, the test container will only get
   * created and started once! {@link EngineContainer} is a Singleton so no new object is created
   * each time. This is done for performance reasons. Starting a testcontainer can take a couple of
   * seconds, so we aim to restart it a minimal amount of times.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void beforeAll(final ExtensionContext extensionContext) {
    final EngineContainer container = EngineContainer.getContainer();
    container.start();

    final ContainerizedEngine engine =
        new ContainerizedEngine(
            container.getHost(),
            container.getMappedPort(ContainerProperties.getContainerPort()),
            container.getMappedPort(ContainerProperties.getGatewayPort()));

    getStore(extensionContext).put(KEY_ZEEBE_ENGINE, engine);
  }

  /**
   * Before each test the {@link ContainerizedEngine} is started. A client to communicate with the
   * engine will be created, together with a {@link RecordStream}. These will be injected in the
   * fields of the test class, if they are available.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void beforeEach(final ExtensionContext extensionContext) {
    final Object engineContent = getStore(extensionContext.getParent().get()).get(KEY_ZEEBE_ENGINE);
    final Optional<ObjectMapper> customObjectMapper = getCustomMapper(extensionContext);
    final ContainerizedEngine engine = (ContainerizedEngine) engineContent;
    engine.start();

    final ZeebeClient client = createClient(customObjectMapper, engine);
    final RecordStream recordStream = RecordStream.of(new RecordStreamSourceImpl(engine));
    BpmnAssert.initRecordStream(recordStream);

    getStore(extensionContext).put(KEY_ZEEBE_CLIENT, client);
    injectFields(extensionContext, engine, client, recordStream);
  }

  /**
   * After each test the client will be closed. At this point we will reset the engine. This will
   * stop the current engine and create a new one. The new engine will not be started yet as that is
   * done in the beforeEach method.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void afterEach(final ExtensionContext extensionContext) {
    final Object clientContent = getStore(extensionContext).get(KEY_ZEEBE_CLIENT);
    final ZeebeClient client = (ZeebeClient) clientContent;
    client.close();

    final Object engineContent = getStore(extensionContext.getParent().get()).get(KEY_ZEEBE_ENGINE);
    final ContainerizedEngine engine = (ContainerizedEngine) engineContent;
    engine.reset();
  }

  /**
   * Upon test failure an overview of occurred events and incidents will be logged.
   *
   * @param extensionContext jUnit5 extension context
   * @param cause the throwable that caused the test failure
   */
  @Override
  public void testFailed(final ExtensionContext extensionContext, final Throwable cause) {
    final Object engineContent = getStore(extensionContext.getParent().get()).get(KEY_ZEEBE_ENGINE);
    final ContainerizedEngine engine = (ContainerizedEngine) engineContent;
    LOG.error("===== Test failed!");
    RecordStream.of(engine.getRecordStreamSource()).print(true);
  }

  private void injectFields(final ExtensionContext extensionContext, final Object... objects) {
    final Class<?> requiredTestClass = extensionContext.getRequiredTestClass();
    for (final Object object : objects) {
      final Optional<Field> field = getField(requiredTestClass, object);
      field.ifPresent(value -> injectField(extensionContext, value, object));
    }
  }

  private Optional<Field> getField(final Class<?> requiredTestClass, final Object object) {
    final Field[] declaredFields = requiredTestClass.getDeclaredFields();

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
    } else if (fields.size() == 0) {
      final Class<?> superclass = requiredTestClass.getSuperclass();
      return superclass == null ? Optional.empty() : getField(superclass, object);
    } else {
      return Optional.of(fields.get(0));
    }
  }

  private void injectField(
      final ExtensionContext extensionContext, final Field field, final Object object) {
    try {
      ReflectionUtils.makeAccessible(field);
      field.set(extensionContext.getRequiredTestInstance(), object);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private ExtensionContext.Store getStore(final ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getUniqueId()));
  }

  /**
   * Get a custom object mapper from the test context
   *
   * @param context jUnit5 extension context
   * @return {@link Optional} of {@link ObjectMapper}, or Optional.empty() if no object mapper are
   *     in the context
   */
  private Optional<ObjectMapper> getCustomMapper(final ExtensionContext context) {
    final Optional<Field> customMapperOpt =
        getField(context.getRequiredTestClass(), objectMapperInstance);
    if (!customMapperOpt.isPresent()) {
      return Optional.empty();
    }
    final Field customMapper = customMapperOpt.get();
    ReflectionUtils.makeAccessible(customMapper);
    try {
      return Optional.of((ObjectMapper) customMapper.get(context.getRequiredTestInstance()));
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create a {@link ZeebeClient}. If a custom {@link ObjectMapper} is provided it is initialized
   * into the {@link ObjectMapperConfig} and it is configured into the client
   *
   * @param objectMapper an {@link Optional} of {@link ObjectMapper}
   * @param engine the used engine
   * @return a zeebe client
   */
  private ZeebeClient createClient(
      final Optional<ObjectMapper> objectMapper, final ContainerizedEngine engine) {
    if (objectMapper.isPresent()) {
      final ObjectMapper customObjectMapper = objectMapper.get();
      ObjectMapperConfig.initialize(customObjectMapper);
      return engine.createClient(customObjectMapper);
    }
    return engine.createClient();
  }
}
