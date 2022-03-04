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
package io.camunda.zeebe.process.test.filters;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MessageRecordStreamFilter {

  private final Stream<Record<MessageRecordValue>> stream;

  public MessageRecordStreamFilter(final Iterable<Record<MessageRecordValue>> messageRecords) {
    stream = StreamSupport.stream(messageRecords.spliterator(), false);
  }

  public MessageRecordStreamFilter(final Stream<Record<MessageRecordValue>> stream) {
    this.stream = stream;
  }

  public MessageRecordStreamFilter withKey(final long key) {
    return new MessageRecordStreamFilter(stream.filter(record -> record.getKey() == key));
  }

  public MessageRecordStreamFilter withMessageName(final String messageName) {
    return new MessageRecordStreamFilter(
        stream.filter(record -> record.getValue().getName().equals(messageName)));
  }

  public MessageRecordStreamFilter withCorrelationKey(final String correlationKey) {
    return new MessageRecordStreamFilter(
        stream.filter(record -> record.getValue().getCorrelationKey().equals(correlationKey)));
  }

  public MessageRecordStreamFilter withIntent(final MessageIntent intent) {
    return new MessageRecordStreamFilter(stream.filter(record -> record.getIntent() == intent));
  }

  public MessageRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new MessageRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public Stream<Record<MessageRecordValue>> stream() {
    return stream;
  }
}
