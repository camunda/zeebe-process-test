package io.camunda.zeebe.bpmnassert.testengine;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;

public interface InMemoryEngine {

  void start();

  void stop();

  RecordStreamSource getRecordStream();

  ZeebeClient createClient();

  String getGatewayAddress();

  void increaseTime(Duration timeToAdd);

  void runOnIdleState(Runnable callback);

  void waitForIdleState();
}
