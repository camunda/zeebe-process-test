package io.camunda.zeebe.bpmnassert.extensions;

import io.camunda.zeebe.bpmnassert.inspections.RecordStreamSourceStore;
import io.camunda.zeebe.client.ZeebeClient;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import org.camunda.community.eze.RecordStreamSource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;

public class ZeebeAssertionsExtension implements BeforeEachCallback, AfterEachCallback {

  private static final String KEY_ZEEBE_CLIENT = "ZEEBE_CLIENT";

  @Override
  public void beforeEach(final ExtensionContext extensionContext) throws Exception {
    storeRecordStreamSource(extensionContext);
    storeZeebeClient(extensionContext);
  }

  @Override
  public void afterEach(final ExtensionContext extensionContext) {
    RecordStreamSourceStore.reset();

    final Object fieldContent = getStore(extensionContext).get(KEY_ZEEBE_CLIENT);
    final ZeebeClient zeebeClient = (ZeebeClient) fieldContent;
    zeebeClient.close();
  }

  private void storeRecordStreamSource(final ExtensionContext extensionContext)
      throws IllegalAccessException {
    final Field recordStreamField = getRecordStreamField(extensionContext);
    ReflectionUtils.makeAccessible(recordStreamField);
    final RecordStreamSource recordStreamSource =
        (RecordStreamSource) recordStreamField.get(extensionContext.getRequiredTestInstance());
    RecordStreamSourceStore.init(recordStreamSource);
  }

  private Field getRecordStreamField(final ExtensionContext extensionContext) {
    final Optional<Field> recordStreamOptional =
        Arrays.stream(extensionContext.getRequiredTestClass().getDeclaredFields())
            .filter(field -> field.getType() == RecordStreamSource.class)
            .findFirst();
    return recordStreamOptional.orElseThrow(
        () ->
            new IllegalStateException(
                "Expected a field of type RecordStreamSource to be declared in the test class, "
                    + "but none has been found. Please make sure a field of type RecordStreamSource"
                    + " has been declared in the test class."));
  }

  private void storeZeebeClient(final ExtensionContext extensionContext)
      throws IllegalAccessException {
    final Field zeebeClientField = getZeebeClientField(extensionContext);
    ReflectionUtils.makeAccessible(zeebeClientField);
    final ZeebeClient zeebeClient =
        (ZeebeClient) zeebeClientField.get(extensionContext.getRequiredTestInstance());
    getStore(extensionContext).put(KEY_ZEEBE_CLIENT, zeebeClient);
  }

  private Field getZeebeClientField(final ExtensionContext extensionContext) {
    final Optional<Field> zeebeClientOptional =
        Arrays.stream(extensionContext.getRequiredTestClass().getDeclaredFields())
            .filter(field -> ZeebeClient.class.isAssignableFrom(field.getType()))
            .findFirst();
    return zeebeClientOptional.orElseThrow(
        () ->
            new IllegalStateException(
                "Expected a field of type ZeebeClient to be declared in the test class, "
                    + "but none has been found. Please make sure a field of type ZeebeClient "
                    + "has been declared in the test class."));
  }

  private ExtensionContext.Store getStore(final ExtensionContext context) {
    return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getUniqueId()));
  }
}
