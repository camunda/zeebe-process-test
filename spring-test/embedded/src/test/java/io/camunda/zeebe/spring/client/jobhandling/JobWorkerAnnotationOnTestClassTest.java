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

import io.camunda.client.annotation.JobWorker;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Regression test verifying that a {@link JobWorker} annotation placed directly on a method of a
 * {@link SpringBootTest} test class is picked up and the worker is registered. This was broken in
 * the 8.8 refactoring where annotation processing switched from a {@code BeanPostProcessor} (which
 * sees all objects including test instances) to iterating {@code
 * ApplicationContext.getBeanDefinitionNames()} (which does not include test classes).
 */
@SpringBootTest(classes = JobWorkerAnnotationOnTestClassTest.class)
@ZeebeSpringTest
public class JobWorkerAnnotationOnTestClassTest {

  private static final AtomicBoolean WORKER_CALLED = new AtomicBoolean(false);

  @Autowired private ZeebeClient client;

  @JobWorker(type = "testClassWorkerType", autoComplete = true)
  public void handleJob(final ActivatedJob job) {
    WORKER_CALLED.set(true);
  }

  @Test
  void testJobWorkerOnTestClassMethodIsRegistered() {
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("jobWorkerTestProcess")
            .startEvent()
            .serviceTask()
            .zeebeJobType("testClassWorkerType")
            .endEvent()
            .done();

    client.newDeployResourceCommand().addProcessModel(process, "jobWorkerTest.bpmn").send().join();

    final ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("jobWorkerTestProcess")
            .latestVersion()
            .send()
            .join();

    waitForProcessInstanceCompleted(processInstance);

    assertThat(WORKER_CALLED).isTrue();
  }
}
