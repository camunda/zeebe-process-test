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
import io.camunda.zeebe.protocol.record.value.deployment.FormMetadataValue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FormStreamFilter {

  private final Stream<Record<FormMetadataValue>> stream;

  public FormStreamFilter(final Iterable<Record<FormMetadataValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public Stream<Record<FormMetadataValue>> stream() {
    return stream;
  }
}
