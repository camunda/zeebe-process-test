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
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IncidentRecordStreamFilter {
  private final Stream<Record<IncidentRecordValue>> stream;

  public IncidentRecordStreamFilter(final Iterable<Record<IncidentRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public IncidentRecordStreamFilter(final Stream<Record<IncidentRecordValue>> stream) {
    this.stream = stream;
  }

  public IncidentRecordStreamFilter withIncidentKey(final long incidentKey) {
    return new IncidentRecordStreamFilter(stream.filter(record -> record.getKey() == incidentKey));
  }

  public IncidentRecordStreamFilter withRejectionType(final RejectionType rejectionType) {
    return new IncidentRecordStreamFilter(
        stream.filter(record -> record.getRejectionType() == rejectionType));
  }

  public IncidentRecordStreamFilter withProcessInstanceKey(final long processInstanceKey) {
    return new IncidentRecordStreamFilter(
        stream.filter(record -> record.getValue().getProcessInstanceKey() == processInstanceKey));
  }

  public IncidentRecordStreamFilter withJobKey(final long jobKey) {
    return new IncidentRecordStreamFilter(
        stream.filter(record -> record.getValue().getJobKey() == jobKey));
  }

  public IncidentRecordStreamFilter withIntent(final IncidentIntent intent) {
    return new IncidentRecordStreamFilter(stream.filter(record -> record.getIntent() == intent));
  }

  public Stream<Record<IncidentRecordValue>> stream() {
    return stream;
  }
}
