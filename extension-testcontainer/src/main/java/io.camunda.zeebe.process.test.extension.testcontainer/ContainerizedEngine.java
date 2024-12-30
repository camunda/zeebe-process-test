/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.process.test.extension.testcontainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import io.camunda.client.impl.CamundaObjectMapper;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlGrpc;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlGrpc.EngineControlBlockingStub;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.GetRecordsRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.IncreaseTimeRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.RecordResponse;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.ResetEngineRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.StartEngineRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.StopEngineRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForBusyStateRequest;
import io.camunda.zeebe.process.test.engine.protocol.EngineControlOuterClass.WaitForIdleStateRequest;
import io.camunda.zeebe.protocol.jackson.ZeebeProtocolModule;
import io.camunda.zeebe.protocol.record.Record;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@link ContainerizedEngine} is used for communicating with the actual test engine running in
 * the testcontainer. Communicating will be done through gRPC. Implementation details for this gRPC
 * service can be found in engine-protocol.
 *
 * <p>This engine is a stripped down version of the actual Zeebe Engine. Its intended purpose is for
 * testing purposes only.
 */
public class ContainerizedEngine implements ZeebeTestEngine {

  private final String host;
  private final int containerPort;
  private final int channelPort;

  public ContainerizedEngine(final String host, final int containerPort, final int channelPort) {
    this.host = host;
    this.containerPort = containerPort;
    this.channelPort = channelPort;
  }

  @Override
  public void start() {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);

    final StartEngineRequest request = StartEngineRequest.newBuilder().build();
    stub.startEngine(request);

    closeChannel(channel);
  }

  @Override
  public void stop() {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);

    final StopEngineRequest request = StopEngineRequest.newBuilder().build();
    stub.stopEngine(request);

    closeChannel(channel);
  }

  @Override
  public RecordStreamSource getRecordStreamSource() {
    return new RecordStreamSourceImpl(this, getRecords());
  }

  @Override
  public CamundaClient createClient() {
    return CamundaClient.newClientBuilder()
        .applyEnvironmentVariableOverrides(false)
        .gatewayAddress(getGatewayAddress())
        .usePlaintext()
        .build();
  }

  @Override
  public CamundaClient createClient(final ObjectMapper objectMapper) {
    return CamundaClient.newClientBuilder()
        .withJsonMapper(new CamundaObjectMapper(objectMapper))
        .applyEnvironmentVariableOverrides(false)
        .gatewayAddress(getGatewayAddress())
        .usePlaintext()
        .build();
  }

  @Override
  public String getGatewayAddress() {
    return host + ":" + channelPort;
  }

  @Override
  public void increaseTime(final Duration timeToAdd) {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);

    final IncreaseTimeRequest request =
        IncreaseTimeRequest.newBuilder().setMilliseconds((int) timeToAdd.toMillis()).build();
    stub.increaseTime(request);

    closeChannel(channel);
  }

  @Override
  public void waitForIdleState(final Duration timeout) {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);

    final WaitForIdleStateRequest request =
        WaitForIdleStateRequest.newBuilder().setTimeout(timeout.toMillis()).build();
    stub.waitForIdleState(request);

    closeChannel(channel);
  }

  @Override
  public void waitForBusyState(final Duration timeout)
      throws InterruptedException, TimeoutException {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);

    final WaitForBusyStateRequest request =
        WaitForBusyStateRequest.newBuilder().setTimeout(timeout.toMillis()).build();
    try {
      stub.waitForBusyState(request);
    } catch (final StatusRuntimeException e) {
      if (e.getStatus().getCode().equals(Status.DEADLINE_EXCEEDED.getCode())) {
        throw new TimeoutException(e.getMessage());
      } else if (e.getStatus().getCode().equals(Status.INTERNAL.getCode())) {
        throw new InterruptedException(e.getMessage());
      }
    } finally {
      closeChannel(channel);
    }
  }

  /**
   * Reset the test engine. A reset stops the current engine, and replaces it with a new engine.
   * Note that you'll need to redeploy your processes because it is an entirely new engine.
   */
  public void reset() {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);

    final ResetEngineRequest request = ResetEngineRequest.newBuilder().build();
    stub.resetEngine(request);

    closeChannel(channel);
  }

  /**
   * Gets a list of all records that have occurred on the test engine.
   *
   * @return a list of records
   */
  public List<Record<?>> getRecords() {
    final ManagedChannel channel = getChannel();
    final EngineControlBlockingStub stub = getStub(channel);
    final ObjectMapper mapper = new ObjectMapper().registerModule(new ZeebeProtocolModule());
    final List<Record<?>> mappedRecords = new ArrayList<>();

    final GetRecordsRequest request = GetRecordsRequest.newBuilder().build();
    final Iterator<RecordResponse> response = stub.getRecords(request);

    while (response.hasNext()) {
      final RecordResponse recordResponse = response.next();
      try {
        final Record<?> record =
            mapper.readValue(recordResponse.getRecordJson(), new TypeReference<Record<?>>() {});
        mappedRecords.add(record);
      } catch (final JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    closeChannel(channel);
    return mappedRecords;
  }

  private ManagedChannel getChannel() {
    return ManagedChannelBuilder.forAddress(host, containerPort).usePlaintext().build();
  }

  private EngineControlBlockingStub getStub(final ManagedChannel channel) {
    return EngineControlGrpc.newBlockingStub(channel);
  }

  private void closeChannel(final ManagedChannel channel) {
    channel.shutdown();
    try {
      channel.awaitTermination(100, TimeUnit.MILLISECONDS);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
