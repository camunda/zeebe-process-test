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
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class VariableRecordStreamFilter {

  private Stream<Record<VariableRecordValue>> stream;

  public VariableRecordStreamFilter(final Iterable<Record<VariableRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public VariableRecordStreamFilter(final Stream<Record<VariableRecordValue>> stream) {
    this.stream = stream;
  }

  public VariableRecordStreamFilter withProcessInstanceKey(final long processInstanceKey) {
    return new VariableRecordStreamFilter(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public VariableRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new VariableRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public Stream<Record<VariableRecordValue>> stream() {
    return stream;
  }
}
