package io.camunda.zeebe.process.test.extension.testcontainer;

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

  /**
   * A new testcontainer container gets created and started. This container will run the test
   * engine. After this a {@link ContainerizedEngine} is created, which allows communicating with
   * the testcontainer.
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
   * Before each test the {@link ContainerizedEngine} is reset. A client to communicate with the
   * engine will be created, together with a {@link RecordStream}. These will be injected in the
   * fields of the test class, if they are available.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void beforeEach(final ExtensionContext extensionContext) {
    final Object engineContent = getStore(extensionContext.getParent().get()).get(KEY_ZEEBE_ENGINE);
    final ContainerizedEngine engine = (ContainerizedEngine) engineContent;
    engine.reset();

    final ZeebeClient client = engine.createClient();
    final RecordStream recordStream = RecordStream.of(new RecordStreamSourceImpl(engine));
    BpmnAssert.initRecordStream(recordStream);

    getStore(extensionContext).put(KEY_ZEEBE_CLIENT, client);
    injectFields(extensionContext, engine, client, recordStream);
  }

  /**
   * After each test the client will be closed.
   *
   * @param extensionContext jUnit5 extension context
   */
  @Override
  public void afterEach(final ExtensionContext extensionContext) {
    final Object clientContent = getStore(extensionContext).get(KEY_ZEEBE_CLIENT);
    final ZeebeClient client = (ZeebeClient) clientContent;
    client.close();
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
