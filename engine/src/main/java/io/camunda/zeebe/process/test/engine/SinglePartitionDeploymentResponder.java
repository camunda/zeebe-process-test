/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.engine.processing.deployment.DeploymentResponder;

public class SinglePartitionDeploymentResponder implements DeploymentResponder {

  @Override
  public void sendDeploymentResponse(final long deploymentKey, final int partitionId) {
    // no need to implement if there is only one partition
  }
}
