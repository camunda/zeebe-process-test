package io.camunda.zeebe.process.test.extensions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.process.test.testengine.RecordStreamSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class ZeebeProcessTestExtensionTest {

  @Nested
  class MultipleInjectedFields {

    private RecordStreamSource recordStreamSourceOne;
    private RecordStreamSource recordStreamSourceTwo;

    @Test
    public void testMultipleInjectedFieldsThrowError() {
      // given
      final ZeebeProcessTestExtension extension = new ZeebeProcessTestExtension();
      final ExtensionContext extensionContext = mock(ExtensionContext.class);

      // when
      Mockito.<Class<?>>when(extensionContext.getRequiredTestClass()).thenReturn(this.getClass());
      Mockito.when(extensionContext.getRequiredTestInstance()).thenReturn(this);

      // then
      assertThatThrownBy(() -> extension.beforeEach(extensionContext))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage(
              "Expected at most one field of type RecordStreamSourceImpl, but found 2. "
                  + "Please make sure at most one field of type RecordStreamSourceImpl has been "
                  + "declared in the test class.");
    }
  }
}
