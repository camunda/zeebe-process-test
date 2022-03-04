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
