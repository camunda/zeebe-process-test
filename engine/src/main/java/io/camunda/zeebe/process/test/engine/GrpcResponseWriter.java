/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.rpc.Status;
import io.camunda.zeebe.process.test.engine.GatewayRequestStore.Request;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.stream.api.CommandResponseWriter;
import io.camunda.zeebe.util.buffer.BufferUtil;
import io.camunda.zeebe.util.buffer.BufferWriter;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

class GrpcResponseWriter implements CommandResponseWriter {

  private static long key = -1;
  private static final DirectBuffer valueBufferView = new UnsafeBuffer();
  private static Intent intent = Intent.UNKNOWN;
  final GrpcToLogStreamGateway gateway;
  private final GatewayRequestStore gatewayRequestStore;
  private int partitionId = -1;
  private RecordType recordType = RecordType.NULL_VAL;
  private ValueType valueType = ValueType.NULL_VAL;
  private RejectionType rejectionType = RejectionType.NULL_VAL;
  private String rejectionReason = "";
  private final MutableDirectBuffer valueBuffer = new ExpandableArrayBuffer();
  private final GrpcResponseMapper responseMapper = new GrpcResponseMapper();
  private final Consumer<Intent> requestListener;

  public GrpcResponseWriter(
      final GrpcToLogStreamGateway gateway,
      final GatewayRequestStore gatewayRequestStore,
      final Consumer<Intent> requestListener) {
    this.gateway = gateway;
    this.gatewayRequestStore = gatewayRequestStore;
    this.requestListener = requestListener;
  }

  @Override
  public CommandResponseWriter partitionId(final int partitionId) {
    this.partitionId = partitionId;
    return this;
  }

  @Override
  public CommandResponseWriter key(final long key) {
    GrpcResponseWriter.key = key;
    return this;
  }

  @Override
  public CommandResponseWriter intent(final Intent intent) {
    GrpcResponseWriter.intent = intent;
    return this;
  }

  @Override
  public CommandResponseWriter recordType(final RecordType type) {
    recordType = type;
    return this;
  }

  @Override
  public CommandResponseWriter valueType(final ValueType valueType) {
    this.valueType = valueType;
    return this;
  }

  @Override
  public CommandResponseWriter rejectionType(final RejectionType rejectionType) {
    this.rejectionType = rejectionType;
    return this;
  }

  @Override
  public CommandResponseWriter rejectionReason(final DirectBuffer rejectionReason) {
    this.rejectionReason = BufferUtil.bufferAsString(rejectionReason);
    return this;
  }

  @Override
  public CommandResponseWriter valueWriter(final BufferWriter value) {
    value.write(valueBuffer, 0);
    valueBufferView.wrap(valueBuffer, 0, value.getLength());
    return this;
  }

  @Override
  public void tryWriteResponse(final int requestStreamId, final long requestId) {
    if (rejectionType != RejectionType.NULL_VAL) {
      final Status rejectionResponse =
          responseMapper.createRejectionResponse(rejectionType, intent, rejectionReason);
      final Request request = gatewayRequestStore.removeRequest(requestId);
      sendError(request, rejectionResponse);
      return;
    }

    try {
      if (requestListener != null) {
        requestListener.accept(intent);
      }
      final Request request = gatewayRequestStore.removeRequest(requestId);
      final GeneratedMessage response =
          responseMapper.map(request.requestType(), valueBufferView, key, intent);
      sendResponse(request, response);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void sendResponse(final Request request, final GeneratedMessage response) {
    final StreamObserver<GeneratedMessage> streamObserver =
        (StreamObserver<GeneratedMessage>) request.responseObserver();
    streamObserver.onNext(response);
    streamObserver.onCompleted();
  }

  private void sendError(final Request request, final Status error) {
    final StreamObserver<GeneratedMessage> streamObserver =
        (StreamObserver<GeneratedMessage>) request.responseObserver();
    streamObserver.onError(StatusProto.toStatusException(error));
  }
}
