package io.camunda.zeebe.process.test.engine.agent;

import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.engine.EngineFactory;

public class ZeebeProcessTestEngine {

  public static void main(String[] args) {
    final InMemoryEngine engine = EngineFactory.create();
    engine.start();
  }
}
