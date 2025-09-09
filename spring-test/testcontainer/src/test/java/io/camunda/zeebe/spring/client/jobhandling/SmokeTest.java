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
package io.camunda.zeebe.spring.client.jobhandling;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.spring.test.ZeebeTestThreadSupport.waitForProcessInstanceCompleted;
import static org.junit.jupiter.api.Assertions.*;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.worker.JobClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
    classes = {SmokeTest.WorkerConfig.class},
    properties = {"camunda.client.worker.defaults.type=DefaultType"})
@ZeebeSpringTest
public class SmokeTest {

  private static boolean calledTest1 = false;
  private static boolean calledTest2 = false;
  private static ComplexTypeDTO test2ComplexTypeDTO = null;
  private static String test2Var2 = null;
  @Autowired private CamundaClient client;
  @Autowired private ZeebeTestEngine engine;

  @Test
  public void testAutoComplete() {
    final BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess("test1")
            .startEvent()
            .serviceTask()
            .zeebeJobType("test1")
            .endEvent()
            .done();

    client.newDeployResourceCommand().addProcessModel(bpmnModel, "test1.bpmn").send().join();

    final Map<String, Object> variables =
        Collections.singletonMap("magicNumber", "42"); // Todo: 42 instead of "42" fails?
    final ProcessInstanceEvent processInstance = startProcessInstance(client, "test1", variables);

    assertThat(processInstance).isStarted();
    waitForProcessInstanceCompleted(processInstance);
    assertTrue(calledTest1);
  }

  @Test
  void testShouldDeserializeComplexTypeZebeeVariable() {
    final String processId = "test2";
    final BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess(processId)
            .startEvent()
            .serviceTask()
            .zeebeJobType(processId)
            .endEvent()
            .done();
    client.newDeployResourceCommand().addProcessModel(bpmnModel, processId + ".bpmn").send().join();

    final ComplexTypeDTO dto = new ComplexTypeDTO();
    dto.setVar1("value1");
    dto.setVar2("value2");

    final Map<String, Object> variables = new HashMap<>();
    variables.put("dto", dto);
    variables.put("var2", "stringValue");

    final ProcessInstanceEvent processInstance = startProcessInstance(client, processId, variables);
    waitForProcessInstanceCompleted(processInstance);

    assertTrue(calledTest2);
    assertNotNull(test2ComplexTypeDTO);
    assertNotEquals(new ComplexTypeDTO(), test2ComplexTypeDTO);
    assertEquals("value1", test2ComplexTypeDTO.getVar1());
    assertEquals("value2", test2ComplexTypeDTO.getVar2());
    assertNotNull(test2Var2);
    assertEquals("stringValue", test2Var2);
  }

  private ProcessInstanceEvent startProcessInstance(
      final CamundaClient client, final String bpmnProcessId) {
    return startProcessInstance(client, bpmnProcessId, new HashMap<>());
  }

  private ProcessInstanceEvent startProcessInstance(
      final CamundaClient client, final String bpmnProcessId, final Map<String, Object> variables) {
    return client
        .newCreateInstanceCommand()
        .bpmnProcessId(bpmnProcessId)
        .latestVersion()
        .variables(variables)
        .send()
        .join();
  }

  @Configuration
  public static class WorkerConfig {
    @JobWorker(name = "test1", type = "test1") // autoComplete is true
    public void handleTest1(final JobClient client, final ActivatedJob job) {
      calledTest1 = true;
    }

    @JobWorker(name = "test2", type = "test2", pollInterval = 10)
    public void handleTest2(
        final JobClient client,
        final ActivatedJob job,
        @Variable final ComplexTypeDTO dto,
        @Variable final String var2) {
      calledTest2 = true;
      test2ComplexTypeDTO = dto;
      test2Var2 = var2;
    }
  }

  private static class ComplexTypeDTO {
    private String var1;
    private String var2;

    public String getVar1() {
      return var1;
    }

    public void setVar1(final String var1) {
      this.var1 = var1;
    }

    public String getVar2() {
      return var2;
    }

    public void setVar2(final String var2) {
      this.var2 = var2;
    }
  }
}
