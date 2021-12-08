package io.camunda.zeebe.process.test.testengine;

import io.camunda.zeebe.engine.processing.deployment.DeploymentResponder;

public class SinglePartitionDeploymentResponder implements DeploymentResponder {

  @Override
  public void sendDeploymentResponse(final long deploymentKey, final int partitionId) {
    // no need to implement if there is only one partition
  }
}
