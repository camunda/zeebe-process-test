/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class GatewayRequestStore {

  private final Map<Long, Request> requestMap = new ConcurrentHashMap<>();
  private final AtomicLong requestIdGenerator = new AtomicLong();

  Long registerNewRequest(
      final Class<? extends GeneratedMessageV3> requestType,
      final StreamObserver<?> responseObserver) {
    final long currentRequestId = requestIdGenerator.incrementAndGet();
    requestMap.put(currentRequestId, new Request(requestType, responseObserver));
    return currentRequestId;
  }

  Request removeRequest(final Long requestId) {
    return requestMap.remove(requestId);
  }

  record Request(
      Class<? extends GeneratedMessageV3> requestType, StreamObserver<?> responseObserver) {}
}
