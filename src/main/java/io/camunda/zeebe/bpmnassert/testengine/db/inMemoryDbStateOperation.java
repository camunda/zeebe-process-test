package io.camunda.zeebe.bpmnassert.testengine.db;

@FunctionalInterface
public interface inMemoryDbStateOperation {

  void run(final InMemoryDbState state) throws Exception;
}
