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
package io.camunda.zeebe.process.test.extension.testcontainer;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import java.util.ArrayList;

/**
 * The source for record processed by the test engine. This class is responsible for getting the
 * records from the test engine and storing them locally.
 */
public class RecordStreamSourceImpl implements RecordStreamSource {

  private final ContainerizedEngine engine;
  private Iterable<Record<?>> records;

  public RecordStreamSourceImpl(final ContainerizedEngine engine) {
    this(engine, new ArrayList<>());
  }

  public RecordStreamSourceImpl(
      final ContainerizedEngine engine, final Iterable<Record<?>> records) {
    this.engine = engine;
    this.records = records;
  }

  /** {@inheritDoc} */
  @Override
  public Iterable<Record<?>> getRecords() {
    updateWithNewRecords();
    return records;
  }

  private void updateWithNewRecords() {
    records = engine.getRecords();
  }
}
