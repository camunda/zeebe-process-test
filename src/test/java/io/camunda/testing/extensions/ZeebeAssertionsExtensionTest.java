package io.camunda.testing.extensions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.client.ZeebeClient;
import java.util.Optional;
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
      Mockito.when(extensionContext.getTestClass()).thenReturn(Optional.of(this.getClass()));
      Mockito.when(extensionContext.getTestInstance()).thenReturn(Optional.of(this));

      // when & then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "No RecordStreamSource has been found. Please make sure a RecordStreamSource "
                  + "field has been declared in the test class.");
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
      Mockito.when(extensionContext.getTestClass()).thenReturn(Optional.of(this.getClass()));
      Mockito.when(extensionContext.getTestInstance()).thenReturn(Optional.of(this));

      // when & then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "No ZeebeClient has been found. Please make sure a ZeebeClient field has been"
                  + " declared in the test class.");
    }
  }
}
