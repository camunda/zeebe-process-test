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

import io.camunda.client.annotation.Deployment;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Regression test verifying that a {@link Deployment} annotation placed directly on a {@link
 * SpringBootTest} test class is picked up and the specified resources are deployed. This was broken
 * in the 8.8 refactoring where annotation processing switched from a {@code BeanPostProcessor}
 * (which sees all objects including test instances) to iterating {@code
 * ApplicationContext.getBeanDefinitionNames()} (which does not include test classes).
 */
@SpringBootTest(classes = DeploymentAnnotationOnTestClassTest.class)
@ZeebeSpringTest
@Deployment(resources = "classpath:deployment-annotation-test.bpmn")
public class DeploymentAnnotationOnTestClassTest {

  @Autowired private ZeebeClient client;

  @Test
  void testProcessDeployedViaClassLevelAnnotation() {
    // If @Deployment on the test class was not processed, this will fail with
    // "Command 'CREATE' rejected: Expected to find process definition with process ID
    // 'deploymentAnnotationTestProcess', but none found"
    final ProcessInstanceEvent processInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("deploymentAnnotationTestProcess")
            .latestVersion()
            .send()
            .join();

    waitForProcessInstanceCompleted(processInstance);
  }
}
