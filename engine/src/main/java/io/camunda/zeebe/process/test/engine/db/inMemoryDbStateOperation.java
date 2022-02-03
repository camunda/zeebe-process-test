package io.camunda.zeebe.process.test.engine.db;

@FunctionalInterface
public interface inMemoryDbStateOperation {

  void run(final InMemoryDbState state) throws Exception;
}
