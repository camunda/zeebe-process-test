/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.engine.processing.deployment.distribute.DeploymentDistributor;
import io.camunda.zeebe.util.sched.future.ActorFuture;
import io.camunda.zeebe.util.sched.future.CompletableActorFuture;
import org.agrona.DirectBuffer;

public class SinglePartitionDeploymentDistributor implements DeploymentDistributor {

  @Override
  public ActorFuture<Void> pushDeploymentToPartition(
      final long key, final int partitionId, final DirectBuffer deploymentBuffer) {
    return CompletableActorFuture.completed(null);
  }
}
