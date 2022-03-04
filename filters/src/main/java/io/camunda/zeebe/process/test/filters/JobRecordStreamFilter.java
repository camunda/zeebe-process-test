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
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JobRecordStreamFilter {

  private final Stream<Record<JobRecordValue>> stream;

  public JobRecordStreamFilter(final Iterable<Record<JobRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public JobRecordStreamFilter(final Stream<Record<JobRecordValue>> stream) {
    this.stream = stream;
  }

  public JobRecordStreamFilter withKey(final long key) {
    return new JobRecordStreamFilter(stream.filter(record -> record.getKey() == key));
  }

  public JobRecordStreamFilter withIntent(final JobIntent intent) {
    return new JobRecordStreamFilter(stream.filter(record -> record.getIntent() == intent));
  }

  public JobRecordStreamFilter withElementId(final String elementId) {
    return new JobRecordStreamFilter(
        stream.filter(record -> record.getValue().getElementId().equals(elementId)));
  }

  public Stream<Record<JobRecordValue>> stream() {
    return stream;
  }
}
