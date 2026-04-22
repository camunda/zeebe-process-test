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

package io.camunda.zeebe.process.test.inspections;

import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.StreamFilter;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.model.xml.instance.DomElement;

public class ProcessDefinitionInspectionUtility {
  /**
   * Finds the BPMN element id by its name.
   *
   * <p>Keeps the test human-readable when asserting on bpmn elements. Asserts that there is only
   * one element for the given name to prevent mistakes.
   *
   * @param bpmnElementName the name of the BPMN element
   * @return the id of the found BPMN element
   */
  public static String getBpmnElementId(String bpmnElementName) {
    return getBpmnElementId(
        StreamFilter.processRecords(BpmnAssert.getRecordStream()).getProcessDefinitions(),
        bpmnElementName);
  }

  /**
   * Finds the BPMN element id by its name inside the referenced BPMN process.
   *
   * <p>Keeps the test human-readable when asserting on bpmn elements. Asserts that there is only
   * one element for the given name to prevent mistakes.
   *
   * @param bpmnProcessId the id of the deployed process
   * @param bpmnElementName the name of the BPMN element
   * @return the id of the found BPMN in the given process
   */
  public static String getBpmnElementId(String bpmnProcessId, String bpmnElementName) {
    return getBpmnElementId(
        StreamFilter.processRecords(BpmnAssert.getRecordStream())
            .withBpmnProcessId(bpmnProcessId)
            .getProcessDefinitions(),
        bpmnElementName);
  }

  /**
   * Finds the BPMN element id by its name inside the referenced deployment.
   *
   * <p>Keeps the test human-readable when asserting on bpmn elements. Asserts that there is only
   * one element for the given name to prevent mistakes.
   *
   * @param deployment
   * @param bpmnElementName
   * @return
   */
  public static String getBpmnElementId(DeploymentEvent deployment, String bpmnElementName) {
    return getBpmnElementId(
        StreamFilter.processRecords(BpmnAssert.getRecordStream())
            .withDeployment(deployment)
            .getProcessDefinitions(),
        bpmnElementName);
  }

  private static String getBpmnElementId(Stream<Process> stream, String bpmnElementName) {
    List<String> potentialElementIds =
        stream
            .map(
                processResource ->
                    Bpmn.readModelFromStream(
                        new ByteArrayInputStream(processResource.getResource())))
            .flatMap(
                bpmnModelInstance ->
                    getChildElementsFlattened(bpmnModelInstance.getDocument().getRootElement())
                        .stream())
            .filter(element -> Objects.equals(element.getAttribute("name"), bpmnElementName))
            .map(element -> element.getAttribute("id"))
            .distinct()
            .collect(Collectors.toList());
    Assertions.assertThat(potentialElementIds).hasSize(1);
    return potentialElementIds.get(0);
  }

  private static Set<DomElement> getChildElementsFlattened(DomElement parent) {
    Set<DomElement> elements = new HashSet<>();
    elements.add(parent);
    parent.getChildElements().forEach(child -> elements.addAll(getChildElementsFlattened(child)));
    return elements;
  }
}
