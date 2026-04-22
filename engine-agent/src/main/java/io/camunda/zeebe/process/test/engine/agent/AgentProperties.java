/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.agent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentProperties {

  private static final Logger LOG = LoggerFactory.getLogger(AgentProperties.class);

  private static final String PROPERTIES_FILE = "/config.properties";
  public static final String GATEWAY_PORT = "gateway.port";
  public static final String CONTROLLER_PORT = "controller.port";
  private static final Properties PROPERTIES = new Properties();

  static {
    try (final InputStream inputStream =
        AgentProperties.class.getResourceAsStream(PROPERTIES_FILE)) {
      PROPERTIES.load(inputStream);
    } catch (NullPointerException e) {
      LOG.error(
          "Could not find property file with name "
              + PROPERTIES_FILE
              + ". Please make sure this property file is available in the resources folder.",
          e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      LOG.error("Could not read properties from file", e);
      throw new RuntimeException(e);
    }
  }

  public static int getControllerPort() {
    return Integer.parseInt(getProperty(CONTROLLER_PORT));
  }

  public static int getGatewayPort() {
    return Integer.parseInt(getProperty(GATEWAY_PORT));
  }

  private static String getProperty(final String property) {
    return PROPERTIES.getProperty(property);
  }
}
