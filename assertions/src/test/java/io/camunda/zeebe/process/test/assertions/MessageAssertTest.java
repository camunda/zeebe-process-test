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
package io.camunda.zeebe.process.test.assertions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageAssertTest {

  @AfterEach
  void afterEach() {
    BpmnAssert.resetRecordStream();
  }

  @Test
  void hasVariableWithValueTest() {
    PublishMessageResponse result = mock(PublishMessageResponse.class);
    doReturn(123L).when(result).getMessageKey();

    ProcessMessageSubscriptionRecordValue recordValue =
        mock(ProcessMessageSubscriptionRecordValue.class);
    doReturn(Collections.singletonMap("key", "value")).when(recordValue).getVariables();
    doReturn(123L).when(recordValue).getMessageKey();

    Record<ProcessMessageSubscriptionRecordValue> record = mock(Record.class);
    doReturn(recordValue).when(record).getValue();
    doReturn(ValueType.PROCESS_MESSAGE_SUBSCRIPTION).when(record).getValueType();
    doReturn(RejectionType.NULL_VAL).when(record).getRejectionType();

    BpmnAssert.initRecordStream(RecordStream.of(() -> Collections.singleton(record)));

    BpmnAssert.assertThat(result).hasVariableWithValue("key", "value");

    assertThatCode(() -> BpmnAssert.assertThat(result).hasVariableWithValue("foo", "value"))
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("Unable to find variable with name 'foo'.");

    assertThatCode(() -> BpmnAssert.assertThat(result).hasVariableWithValue("key", "value2"))
        .isInstanceOf(AssertionError.class)
        .hasMessageStartingWith("The variable 'key' does not have the expected value.");
  }
}
