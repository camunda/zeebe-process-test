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
package io.camunda.zeebe.spring.test;

import io.camunda.zeebe.process.test.extension.testcontainer.ContainerProperties;
import io.camunda.zeebe.process.test.extension.testcontainer.ContainerizedEngine;
import io.camunda.zeebe.process.test.extension.testcontainer.EngineContainer;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/** Test execution listener binding the Zeebe engine to current test context. */
public class ZeebeTestExecutionListener extends AbstractZeebeTestExecutionListener
    implements TestExecutionListener, Ordered {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ContainerizedEngine containerizedEngine;

  public void beforeTestClass(@NonNull TestContext testContext) {
    LOGGER.info("Creating Zeebe Testcontainer...");

    final EngineContainer container = EngineContainer.getContainer();
    container.start();
    containerizedEngine =
        new ContainerizedEngine(
            container.getHost(),
            container.getMappedPort(ContainerProperties.getContainerPort()),
            container.getMappedPort(ContainerProperties.getGatewayPort()));

    LOGGER.info("...finished creating Zeebe Testcontainer");
  }

  public void beforeTestMethod(@NonNull TestContext testContext) {
    LOGGER.info("Create Zeebe Testcontainer engine");
    containerizedEngine.start();
    setupWithZeebeEngine(testContext, containerizedEngine);
  }

  public void afterTestMethod(@NonNull TestContext testContext) {
    cleanup(testContext, containerizedEngine);
    containerizedEngine.reset();
  }

  @Override
  public int getOrder() {
    return Integer.MAX_VALUE;
  }
}
