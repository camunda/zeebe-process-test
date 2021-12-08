package io.camunda.zeebe.process.test.testengine.db;

@FunctionalInterface
public interface inMemoryDbStateOperation {

  void run(final InMemoryDbState state) throws Exception;
}
