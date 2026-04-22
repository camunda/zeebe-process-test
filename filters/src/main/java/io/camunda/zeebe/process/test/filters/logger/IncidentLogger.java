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
package io.camunda.zeebe.process.test.filters.logger;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncidentLogger {

  private static final Logger LOG = LoggerFactory.getLogger(IncidentLogger.class);

  private final RecordStream recordStream;

  public IncidentLogger(final RecordStreamSource recordStreamSource) {
    this.recordStream = RecordStream.of(recordStreamSource);
  }

  public void log() {
    final StringBuilder stringBuilder = new StringBuilder();
    logIncidents(stringBuilder);
    LOG.info(stringBuilder.toString());
  }

  private void logIncidents(final StringBuilder stringBuilder) {
    final Set<Long> resolvedIncidents =
        StreamFilter.incident(recordStream).withIntent(IncidentIntent.RESOLVED).stream()
            .map(Record::getKey)
            .collect(Collectors.toSet());
    final List<Record<IncidentRecordValue>> createIncidents =
        StreamFilter.incident(recordStream).withIntent(IncidentIntent.CREATED).stream()
            .filter(record -> !resolvedIncidents.contains(record.getKey()))
            .collect(Collectors.toList());

    if (!createIncidents.isEmpty()) {
      stringBuilder
          .append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("Unresolved incident(s) exist at the end of this test")
          .append(System.lineSeparator());
      createIncidents.forEach(
          record -> {
            if (!resolvedIncidents.contains(record.getKey())) {
              stringBuilder.append(summarizeIncident(record)).append(System.lineSeparator());
            }
          });
      stringBuilder
          .append(System.lineSeparator())
          .append(
              "If you did not expect any incidents to occur, then we recommend investigating "
                  + "these. These incidents may indicate what went wrong in your test case")
          .append(System.lineSeparator());
    }
  }

  private String summarizeIncident(final Record<IncidentRecordValue> incident) {
    final IncidentRecordValue value = incident.getValue();
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
        .append(
            String.format(
                "On element %s in process %s", value.getElementId(), value.getBpmnProcessId()))
        .append(System.lineSeparator())
        .append("\t")
        .append("- Error type: ")
        .append(value.getErrorType())
        .append(System.lineSeparator())
        .append("\t")
        .append("- Error message: ")
        .append(value.getErrorMessage());
    return stringBuilder.toString();
  }
}
