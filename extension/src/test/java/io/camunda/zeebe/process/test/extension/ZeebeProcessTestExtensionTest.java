/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.extension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.process.test.filters.RecordStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

class ZeebeProcessTestExtensionTest {

  @Nested
  class MultipleInjectedFields {

    private RecordStream recordStreamOne;
    private RecordStream recordStreamTwo;

    @Test
    void testMultipleInjectedFieldsThrowError() {
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
              "Expected at most one field of type RecordStream, but found 2. "
                  + "Please make sure at most one field of type RecordStream has been "
                  + "declared in the test class.");
    }
  }
}
