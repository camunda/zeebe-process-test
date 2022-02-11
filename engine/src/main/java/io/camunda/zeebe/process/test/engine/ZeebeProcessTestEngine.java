package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.process.test.api.InMemoryEngine;

public class ZeebeProcessTestEngine {

  public static void main(String[] args) {
    final InMemoryEngine engine = EngineFactory.create();
    engine.start();
  }
}
