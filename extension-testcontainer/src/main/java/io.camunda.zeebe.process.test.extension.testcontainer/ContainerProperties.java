package io.camunda.zeebe.process.test.extension.testcontainer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContainerProperties {

  private static final Logger LOG = LoggerFactory.getLogger("io.camunda.zeebe-process-test");

  private static final String PROPERTIES_FILE = "/config.properties";
  public static final String IMAGE_NAME = "container.image.name";
  public static final String IMAGE_TAG = "container.image.tag";
  public static final String GATEWAY_PORT = "container.gateway.port";
  public static final String PORT = "container.port";
  private static final Properties PROPERTIES = new Properties();

  static {
    try (final InputStream inputStream =
        ContainerProperties.class.getResourceAsStream(PROPERTIES_FILE)) {
      PROPERTIES.load(inputStream);
    } catch (FileNotFoundException e) {
      LOG.error("Could not find property file with name " + PROPERTIES_FILE, e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      LOG.error("Could not read properties from file", e);
      throw new RuntimeException(e);
    }
  }

  public static String getDockerImageName() {
    return getProperty(IMAGE_NAME) + ":" + getProperty(IMAGE_TAG);
  }

  public static int getContainerPort() {
    return Integer.parseInt(getProperty(PORT));
  }

  public static int getGatewayPort() {
    return Integer.parseInt(getProperty(GATEWAY_PORT));
  }

  private static String getProperty(final String property) {
    return PROPERTIES.getProperty(property);
  }
}
