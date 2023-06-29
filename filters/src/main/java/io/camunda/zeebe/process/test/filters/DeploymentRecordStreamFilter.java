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
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.DeploymentResource;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DeploymentRecordStreamFilter {
  private static final String BPMN_FILE_SUFFIX = ".bpmn";
  private static final String DMN_FILE_SUFFIX = ".dmn";
  private final Stream<Record<DeploymentRecordValue>> stream;

  public DeploymentRecordStreamFilter(final Iterable<Record<DeploymentRecordValue>> records) {
    stream = StreamSupport.stream(records.spliterator(), false);
  }

  public DeploymentRecordStreamFilter(final Stream<Record<DeploymentRecordValue>> stream) {
    this.stream = stream;
  }

  public Stream<DeploymentResource> getDeploymentResources() {
    return stream.flatMap(record -> record.getValue().getResources().stream());
  }

  public Stream<DeploymentResource> getBpmnDeploymentResources() {
    return getDeploymentResources()
        .filter(
            deploymentResource -> deploymentResource.getResourceName().endsWith(BPMN_FILE_SUFFIX));
  }

  public Stream<DeploymentResource> getDmnDeploymentResources() {
    return getDeploymentResources()
        .filter(
            deploymentResource -> deploymentResource.getResourceName().endsWith(DMN_FILE_SUFFIX));
  }

  public DeploymentRecordStreamFilter withBpmnProcessId(String bpmnProcessId) {
    return new DeploymentRecordStreamFilter(
        stream.filter(
            record ->
                record.getValue().getProcessesMetadata().stream()
                    .anyMatch(
                        processMetadataValue ->
                            Objects.equals(
                                processMetadataValue.getBpmnProcessId(), bpmnProcessId))));
  }

  public Stream<Record<DeploymentRecordValue>> stream() {
    return stream;
  }
}
