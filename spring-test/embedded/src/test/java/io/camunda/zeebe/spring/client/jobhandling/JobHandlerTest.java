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

import static io.camunda.zeebe.spring.test.ZeebeTestThreadSupport.waitForProcessInstanceCompleted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.camunda.client.annotation.customizer.JobWorkerValueCustomizer;
import io.camunda.client.annotation.value.JobWorkerValue;
import io.camunda.client.annotation.value.SourceAware;
import io.camunda.client.annotation.value.SourceAware.FromOverrideProperty;
import io.camunda.client.jobhandling.JobWorkerManager;
import io.camunda.client.spring.configuration.MetricsDefaultConfiguration;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.builder.ServiceTaskBuilder;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import io.camunda.zeebe.spring.test.SimpleMetricsRecorder;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
    classes = {
      JobHandlerTest.class,
      JobHandlerTest.WorkerConfig.class,
      JobHandlerTest.TestMetricsConfiguration.class,
      JobHandlerTest.ZeebeCustomizerDisableWorkerConfiguration.class
    })
@ZeebeSpringTest
public class JobHandlerTest {

  private static boolean calledTest1 = false;
  private static boolean calledTest2 = false;
  private static boolean calledTest3 = false;
  private static boolean calledTest4 = false;
  private static boolean calledTest6 = false;
  private static boolean calledTest7 = false;
  private static String test7Var = null;
  private static ComplexTypeDTO test6ComplexTypeDTO = null;
  private static String test6Var2 = null;
  @Autowired private SimpleMetricsRecorder metrics;
  @Autowired private ZeebeClient client;
  @Autowired private JobWorkerManager jobWorkerManager;

  @Test
  public void testAutoComplete() {
    final long activatedAtStart =
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB, SimpleMetricsRecorder.ACTION_COMPLETED, "test1");
    final long completedAtStart =
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB, SimpleMetricsRecorder.ACTION_COMPLETED, "test1");
    final long failedAtStart =
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB, SimpleMetricsRecorder.ACTION_FAILED, "test1");
    final long bpmnErrorAtStart =
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB,
            SimpleMetricsRecorder.ACTION_BPMN_ERROR,
            "test1");

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

    waitForProcessInstanceCompleted(processInstance);
    assertTrue(calledTest1);

    assertEquals(
        activatedAtStart + 1,
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB,
            SimpleMetricsRecorder.ACTION_ACTIVATED,
            "test1"));
    assertEquals(
        completedAtStart + 1,
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB,
            SimpleMetricsRecorder.ACTION_COMPLETED,
            "test1"));
    assertEquals(
        failedAtStart,
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB, SimpleMetricsRecorder.ACTION_FAILED, "test1"));
    assertEquals(
        bpmnErrorAtStart,
        metrics.getCount(
            SimpleMetricsRecorder.METRIC_NAME_JOB,
            SimpleMetricsRecorder.ACTION_BPMN_ERROR,
            "test1"));
  }

  @Test
  public void testWorkerDefaultName() {
    assertThat(jobWorkerManager.getJobWorker("test2"))
        .extracting(JobWorkerValue::getName)
        .extracting(SourceAware::value)
        .isEqualTo("jobHandlerTest.WorkerConfig#handleTest2");
  }

  @Test
  public void testAutoCompleteOnAlreadyCompletedJob() {
    final BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess("test2")
            .startEvent()
            .serviceTask()
            .zeebeJobType("test2")
            .endEvent()
            .done();
    client.newDeployResourceCommand().addProcessModel(bpmnModel, "test2.bpmn").send().join();
    final ProcessInstanceEvent processInstance = startProcessInstance(client, "test2");
    // assertThat(processInstance).isStarted();
    waitForProcessInstanceCompleted(processInstance);
    assertTrue(calledTest2);
  }

  @Test
  void shouldNotActivateJobInAnnotationDisabledWorker() {
    final String processId = "test3";
    final ServiceTaskBuilder serviceTaskBuilder =
        Bpmn.createExecutableProcess(processId).startEvent().serviceTask().zeebeJobType(processId);
    // At the first we are creating the timer boundary event - if we aren't activated for 100 ms -
    // we end the test.
    serviceTaskBuilder
        .boundaryEvent()
        .timerWithDuration(Duration.ofMillis(100).toString())
        .endEvent();
    // But if we broke something and the job is successfully activated - we are throwing the
    // "shouldNotPass" error thus the process instance will never be completed positively if we are
    // going next on this branch.
    final BpmnModelInstance bpmnModelInstance =
        serviceTaskBuilder.endEvent().error("shouldNotPass").done();
    client
        .newDeployResourceCommand()
        .addProcessModel(bpmnModelInstance, "test3.bpmn")
        .send()
        .join();
    final ProcessInstanceEvent processInstance = startProcessInstance(client, processId);
    waitForProcessInstanceCompleted(processInstance);
    // The double-check that we didn't go to the worker.
    assertThat(calledTest3).isFalse();
  }

  /**
   * Worker disabled in {@link ZeebeCustomizerDisableWorkerConfiguration#jobWorkerValueCustomizer()}
   */
  @Test
  void shouldNotActivateJobInPropertiesDisabledWorker() {
    final String processId = "test4";
    final ServiceTaskBuilder serviceTaskBuilder =
        Bpmn.createExecutableProcess(processId).startEvent().serviceTask().zeebeJobType(processId);
    // At the first we are creating the timer boundary event - if we aren't activated for 100 ms -
    // we end the test.
    serviceTaskBuilder
        .boundaryEvent()
        .timerWithDuration(Duration.ofMillis(100).toString())
        .endEvent();
    // But if we broke something and the job is successfully activated - we are throwing the
    // "shouldNotPass" error thus the process instance will never be completed positively if we are
    // going next on this branch.
    final BpmnModelInstance bpmnModelInstance =
        serviceTaskBuilder.endEvent().error("shouldNotPass").done();
    client
        .newDeployResourceCommand()
        .addProcessModel(bpmnModelInstance, "test4.bpmn")
        .send()
        .join();
    final ProcessInstanceEvent processInstance = startProcessInstance(client, processId);
    waitForProcessInstanceCompleted(processInstance);
    // The double-check that we didn't go to the worker.
    assertThat(calledTest4).isFalse();
  }

  @Test
  void testShouldDeserializeComplexTypeZebeeVariable() {
    final String processId = "test6";
    final BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess(processId)
            .startEvent()
            .serviceTask()
            .zeebeJobType(processId)
            .endEvent()
            .done();
    client.newDeployResourceCommand().addProcessModel(bpmnModel, processId + ".bpmn").send().join();
    final Map<String, Object> variables =
        Map.of("dto", Map.of("var1", "value1", "var2", "value2"), "var2", "stringValue");
    final ProcessInstanceEvent processInstance = startProcessInstance(client, processId, variables);
    waitForProcessInstanceCompleted(processInstance);

    assertTrue(calledTest6);
    assertNotNull(test6ComplexTypeDTO);
    assertNotEquals(new ComplexTypeDTO(), test6ComplexTypeDTO);
    assertEquals("value1", test6ComplexTypeDTO.getVar1());
    assertEquals("value2", test6ComplexTypeDTO.getVar2());
    assertNotNull(test6Var2);
    assertEquals("stringValue", test6Var2);
  }

  @Test
  void shouldInjectVariableWithKeywordAsName() {
    final BpmnModelInstance bpmnModel =
        Bpmn.createExecutableProcess("test7")
            .startEvent()
            .serviceTask()
            .zeebeJobType("test7")
            .endEvent()
            .done();
    client.newDeployResourceCommand().addProcessModel(bpmnModel, "test7.bpmn").send().join();
    final ProcessInstanceEvent processInstance =
        startProcessInstance(client, "test7", Map.of("class", "alpha"));
    waitForProcessInstanceCompleted(processInstance);
    assertTrue(calledTest7);
    assertNotNull(test7Var);
    assertEquals("alpha", test7Var);
  }

  private ProcessInstanceEvent startProcessInstance(
      final ZeebeClient client, final String bpmnProcessId) {
    return startProcessInstance(client, bpmnProcessId, new HashMap<>());
  }

  private ProcessInstanceEvent startProcessInstance(
      final ZeebeClient client, final String bpmnProcessId, final Map<String, Object> variables) {
    return client
        .newCreateInstanceCommand()
        .bpmnProcessId(bpmnProcessId)
        .latestVersion()
        .variables(variables)
        .send()
        .join();
  }

  @TestConfiguration
  public static class ZeebeCustomizerDisableWorkerConfiguration {
    @Bean
    public JobWorkerValueCustomizer jobWorkerValueCustomizer() {
      return zeebeWorker -> {
        if (zeebeWorker.getType().value().equals("test4")) {
          zeebeWorker.setEnabled(new FromOverrideProperty<>(false));
        }
      };
    }
  }

  @TestConfiguration
  @AutoConfigureBefore(MetricsDefaultConfiguration.class)
  public static class TestMetricsConfiguration {
    @Bean(name = "noopMetricsRecorder")
    public SimpleMetricsRecorder testMetricsRecorder() {
      return new SimpleMetricsRecorder();
    }
  }

  @TestConfiguration
  public static class WorkerConfig {
    @JobWorker(name = "test1", type = "test1", autoComplete = true)
    public void handleTest1(final JobClient client, final ActivatedJob job) {
      calledTest1 = true;
    }

    @JobWorker(type = "test2", autoComplete = true)
    public void handleTest2(final JobClient client, final ActivatedJob job) {
      // Complete it here to trigger a not found in the auto complete, which will be ignored
      client.newCompleteCommand(job.getKey()).send().join();
      calledTest2 = true;
    }

    @JobWorker(
        name = "test3",
        type = "test3",
        autoComplete = true,
        pollInterval = 10,
        enabled = false)
    public void handeTest3Disabled(final JobClient client, final ActivatedJob job) {
      calledTest3 = true;
    }

    @JobWorker(name = "test4", type = "test4", autoComplete = true, pollInterval = 10)
    public void handeTest4(final JobClient client, final ActivatedJob job) {
      calledTest4 = true;
    }

    @JobWorker(name = "test5", autoComplete = false)
    public void handeTest5() {}

    @JobWorker(name = "test6", type = "test6", pollInterval = 10)
    public void handleTest6(
        final JobClient client,
        final ActivatedJob job,
        @Variable final ComplexTypeDTO dto,
        @Variable final String var2) {
      calledTest6 = true;
      test6ComplexTypeDTO = dto;
      test6Var2 = var2;
    }

    @JobWorker(type = "test7")
    public void handleTest7(@Variable(name = "class") final String variableWithKeywordAsName) {
      calledTest7 = true;
      test7Var = variableWithKeywordAsName;
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

  @SpringBootTest(
      classes = {
        DefaultWorkerTest.class,
        DefaultWorkerTest.WorkersConfig.class,
        JobHandlerTest.TestMetricsConfiguration.class
      },
      properties = {"camunda.client.worker.defaults.type=DefaultType"})
  @ZeebeSpringTest
  @Nested
  final class DefaultWorkerTest {
    @Autowired private JobWorkerManager jobWorkerManager;

    @Test
    public void testWorkerDefaultType() {
      assertDoesNotThrow(() -> jobWorkerManager.getJobWorker("DefaultType"));
    }

    public static class WorkersConfig {
      @JobWorker
      public void handleDefault(@Variable(name = "class") final String variableWithKeywordAsName) {}
    }
  }
}
