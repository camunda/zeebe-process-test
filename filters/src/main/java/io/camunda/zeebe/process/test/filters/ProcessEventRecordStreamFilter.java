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
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessEventIntent;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProcessEventRecordStreamFilter {

  private final Stream<Record<ProcessEventRecordValue>> stream;

  public ProcessEventRecordStreamFilter(final Iterable<Record<?>> records) {
    stream =
        StreamSupport.stream(records.spliterator(), true)
            .filter(record -> record.getValueType() == ValueType.PROCESS_EVENT)
            .map(record -> (Record<ProcessEventRecordValue>) record);
  }

  public ProcessEventRecordStreamFilter(final Stream<Record<ProcessEventRecordValue>> stream) {
    this.stream = stream;
  }

  public ProcessEventRecordStreamFilter withIntent(final ProcessEventIntent intent) {
    return new ProcessEventRecordStreamFilter(
        stream.filter(record -> record.getIntent() == intent));
  }

  public ProcessEventRecordStreamFilter withTargetElementId(final String targetElementId) {
    return new ProcessEventRecordStreamFilter(
        stream.filter(record -> record.getValue().getTargetElementId().equals(targetElementId)));
  }

  public ProcessEventRecordStreamFilter withProcessDefinitionKey(final long processDefinitionKey) {
    return new ProcessEventRecordStreamFilter(
        stream.filter(
            record -> record.getValue().getProcessDefinitionKey() == processDefinitionKey));
  }

  public Stream<Record<ProcessEventRecordValue>> stream() {
    return stream;
  }
}
