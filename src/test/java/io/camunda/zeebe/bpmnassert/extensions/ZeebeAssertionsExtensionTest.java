package io.camunda.zeebe.bpmnassert.extensions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.bpmnassert.testengine.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class ZeebeAssertionsExtensionTest {

  @Nested
  class MultipleInjectedFields {

    private RecordStreamSource recordStreamSourceOne;
    private RecordStreamSource recordStreamSourceTwo;

    @Test
    public void testMultipleInjectedFieldsThrowError() {
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
              "Expected at most one field of type RecordStreamSourceImpl, but found 2. "
                  + "Please make sure at most one field of type RecordStreamSourceImpl has been "
                  + "declared in the test class.");
    }
  }
}
