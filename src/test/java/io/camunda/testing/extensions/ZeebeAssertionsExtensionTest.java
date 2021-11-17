package io.camunda.testing.extensions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.client.ZeebeClient;
import org.camunda.community.eze.RecordStreamSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class ZeebeAssertionsExtensionTest {

  @Nested
  class MissingRecordStreamSource {

    private ZeebeClient client;

    @Test
    public void testExtensionThrowsExceptionWhenMissingRecordStreamSource() {
      // given
      final ZeebeAssertionsExtension extension = new ZeebeAssertionsExtension();
      final ExtensionContext extensionContext = mock(ExtensionContext.class);
      Mockito.<Class<?>>when(extensionContext.getRequiredTestClass()).thenReturn(this.getClass());
      Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(this);

      // when & then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "Expected a field of type RecordStreamSource to be declared in the test class, "
                  + "but none has been found");
    }
  }

  @Nested
  class MissingZeebeClient {

    private RecordStreamSource recordStreamSource;

    @Test
    public void testExtensionThrowsExceptionWhenMissingRecordStreamSource() {
      // given
      final ZeebeAssertionsExtension extension = new ZeebeAssertionsExtension();
      final ExtensionContext extensionContext = mock(ExtensionContext.class);
      Mockito.<Class<?>>when(extensionContext.getRequiredTestClass()).thenReturn(this.getClass());
      Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(this);

      // when & then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "Expected a field of type ZeebeClient to be declared in the test class, "
                  + "but none has been found");
    }
  }
}
