/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine.agent;

import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.engine.EngineFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class ZeebeProcessTestEngine {

  public static void main(final String[] args) throws IOException {
    final ZeebeTestEngine engine = EngineFactory.create(AgentProperties.getGatewayPort());
    final EngineControlImpl engineService = new EngineControlImpl(engine);
    final Server server =
        ServerBuilder.forPort(AgentProperties.getControllerPort())
            .addService(engineService)
            .build();

    engine.start();
    server.start();
  }
}
