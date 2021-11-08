package io.camunda.testing.extensions;

import io.camunda.testing.assertions.BpmnAssertions;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import org.camunda.community.eze.RecordStreamSource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;

// TODO rewrite this mess
public class ZeebeAssertionsExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(final ExtensionContext extensionContext) throws Exception {
    final Optional<Field> recordStreamOptional =
        Arrays.stream(extensionContext.getTestClass().get().getDeclaredFields())
            .filter(field -> field.getType() == RecordStreamSource.class)
            .findFirst();
    final Field recordStreamField = recordStreamOptional.get();
    ReflectionUtils.makeAccessible(recordStreamField);
    final Object testInstance = extensionContext.getTestInstance().get();
    RecordStreamSource recordStreamSource =
        (RecordStreamSource) recordStreamField.get(testInstance);
    BpmnAssertions.init(recordStreamSource);
  }

  @Override
  public void afterEach(final ExtensionContext extensionContext) {
    BpmnAssertions.tearDown();
  }
}
