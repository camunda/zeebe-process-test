package io.camunda.zeebe.process.test.engine.agent;

import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.engine.EngineFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class ZeebeProcessTestEngine {

  public static void main(String[] args) throws IOException {
    final InMemoryEngine engine = EngineFactory.create();
    final EngineControlImpl engineService = new EngineControlImpl(engine);
    final Server server = ServerBuilder.forPort(26501).addService(engineService).build();

    engine.start();
    server.start();
  }
}
