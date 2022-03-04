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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

/** Singleton object which manages access to the testcontainer that's running the test engine */
public final class EngineContainer extends GenericContainer<EngineContainer> {

  private static final Logger LOG = LoggerFactory.getLogger("io.camunda.zeebe-process-test");

  private static EngineContainer INSTANCE;

  private EngineContainer(final String imageName) {
    super(DockerImageName.parse(imageName));
  }

  /**
   * Gets the testcontainer if it exists, else creates it
   *
   * @return the testcontainer
   */
  public static EngineContainer getContainer() {
    if (INSTANCE == null) {
      createContainer();
    }
    return INSTANCE;
  }

  private static void createContainer() {
    INSTANCE =
        new EngineContainer(ContainerProperties.getDockerImageName())
            .withExposedPorts(
                ContainerProperties.getContainerPort(), ContainerProperties.getGatewayPort())
            .withLogConsumer(new Slf4jLogConsumer(LOG));
  }
}
