package io.camunda.zeebe.process.test.extension.testcontainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

public final class EngineContainer extends GenericContainer<EngineContainer> {

  private static EngineContainer INSTANCE;
  private static final Logger LOG = LoggerFactory.getLogger("io.camunda.zeebe-process-test");
  // TODO make this stuff configurable
  private static final DockerImageName IMAGE_NAME = DockerImageName.parse("camunda/zeebe-process-test-engine:latest");
  public static final int GATEWAY_PORT = 26500;
  public static final int CONTAINER_PORT = 26501;

  private EngineContainer() {
    super(IMAGE_NAME);
  }

  public static EngineContainer getContainer() {
    if (INSTANCE == null) {
      createContainer();
    }
    return INSTANCE;
  }

  private static void createContainer() {
    INSTANCE =
        new EngineContainer()
            .withExposedPorts(CONTAINER_PORT, GATEWAY_PORT)
            .withLogConsumer(new Slf4jLogConsumer(LOG));
  }
}
