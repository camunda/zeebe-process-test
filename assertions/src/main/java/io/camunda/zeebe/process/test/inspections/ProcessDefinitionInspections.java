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

import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.process.test.filters.ProcessDefinitionRecordStreamFilter;
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

/**
 * Inspections for deployment events. This is useful to find technical identifiers for
 * human-readable element names.
 */
public class ProcessDefinitionInspections {
  private final ProcessDefinitionRecordStreamFilter processDefinitionRecordStreamFilter;

  public ProcessDefinitionInspections(
      final ProcessDefinitionRecordStreamFilter deploymentRecordStreamFilter) {
    this.processDefinitionRecordStreamFilter = deploymentRecordStreamFilter;
  }

  /**
   * Finds the BPMN element id by its name.
   *
   * <p>Keeps the test human-readable when asserting on bpmn elements. Asserts that there is only
   * one element for the given name to prevent mistakes.
   *
   * @param elementName the name of the BPMN element
   * @return the id of the found BPMN element
   */
  public String findBpmnElementId(String elementName) {
    return findElementId(processDefinitionRecordStreamFilter.getProcessDefinitions(), elementName);
  }

  /**
   * Finds the BPMN element id by its name inside the referenced BPMN process.
   *
   * <p>Keeps the test human-readable when asserting on bpmn elements. Asserts that there is only
   * one element for the given name to prevent mistakes.
   *
   * @param bpmnProcessId the id of the deployed process
   * @param elementName the name of the BPMN element
   * @return the id of the found BPMN in the given process
   */
  public String findBpmnElementId(String bpmnProcessId, String elementName) {
    return findElementId(
        processDefinitionRecordStreamFilter
            .withBpmnProcessId(bpmnProcessId)
            .getProcessDefinitions(),
        elementName);
  }

  private String findElementId(Stream<Process> stream, String elementName) {
    List<String> potentialElementIds =
        stream
            .map(
                processResource ->
                    Bpmn.readModelFromStream(
                        new ByteArrayInputStream(processResource.getResource())))
            .flatMap(
                bpmnModelInstance ->
                    getChildElements(bpmnModelInstance.getDocument().getRootElement()).stream())
            .filter(element -> Objects.equals(element.getAttribute("name"), elementName))
            .map(element -> element.getAttribute("id"))
            .distinct()
            .collect(Collectors.toList());
    Assertions.assertThat(potentialElementIds).hasSize(1);
    return potentialElementIds.get(0);
  }

  private static Set<DomElement> getChildElements(DomElement parent) {
    Set<DomElement> elements = new HashSet<>();
    elements.add(parent);
    parent.getChildElements().forEach(child -> elements.addAll(getChildElements(child)));
    return elements;
  }
}
