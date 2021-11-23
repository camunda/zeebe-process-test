package io.camunda.zeebe.bpmnassert.extensions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.camunda.community.eze.RecordStreamSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class ZeebeAssertionsExtensionTest {

  @Nested
  class MissingRecordStreamSource {

    @Test
    public void testExtensionThrowsExceptionWhenMissingRecordStreamSource() {
      // given
      final ZeebeAssertionsExtension extension = new ZeebeAssertionsExtension();
      final ExtensionContext extensionContext = mock(ExtensionContext.class);

      // when
      Mockito.<Class<?>>when(extensionContext.getRequiredTestClass()).thenReturn(this.getClass());
      Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(this);

      // then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "Expected a field of type RecordStreamSource to be declared in the test class, "
                  + "but none has been found. Please make sure a field of type RecordStreamSource"
                  + " has been declared in the test class.");
    }
  }

  @Nested
  class MissingZeebeClient {

    // In order to test that the ZeebeClient is missing all the other required fields must be
    // present, even though they are unused in this test
    private RecordStreamSource recordStreamSource;

    @Test
    public void testExtensionThrowsExceptionWhenMissingZeebeClient() {
      // given
      final ZeebeAssertionsExtension extension = new ZeebeAssertionsExtension();
      final ExtensionContext extensionContext = mock(ExtensionContext.class);

      // when
      Mockito.<Class<?>>when(extensionContext.getRequiredTestClass()).thenReturn(this.getClass());
      Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(this);

      // then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "Expected a field of type ZeebeClient to be declared in the test class, "
                  + "but none has been found. Please make sure a field of type ZeebeClient has been "
                  + "declared in the test class.");
    }
  }
}
