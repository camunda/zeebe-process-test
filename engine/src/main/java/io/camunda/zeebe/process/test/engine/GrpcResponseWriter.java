/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.camunda.zeebe.engine.processing.streamprocessor.writers.CommandResponseWriter;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivatedJob;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CancelProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CompleteJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceWithResultResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DecisionMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DecisionRequirementsMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployProcessResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceResponse.Builder;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ProcessMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.PublishMessageResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.SetVariablesResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ThrowErrorResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobRetriesResponse;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentRecord;
import io.camunda.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobBatchRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceResultRecord;
import io.camunda.zeebe.protocol.impl.record.value.variable.VariableDocumentRecord;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.util.buffer.BufferUtil;
import io.camunda.zeebe.util.buffer.BufferWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

class GrpcResponseWriter implements CommandResponseWriter {

  private static long key = -1;
  private static final DirectBuffer valueBufferView = new UnsafeBuffer();
  private static Intent intent = Intent.UNKNOWN;
  final GrpcToLogStreamGateway gateway;
  private int partitionId = -1;
  private RecordType recordType = RecordType.NULL_VAL;
  private ValueType valueType = ValueType.NULL_VAL;
  private RejectionType rejectionType = RejectionType.NULL_VAL;
  private String rejectionReason = "";
  private final MutableDirectBuffer valueBuffer = new ExpandableArrayBuffer();
  private final GrpcResponseMapper responseMapper = new GrpcResponseMapper();

  public GrpcResponseWriter(final GrpcToLogStreamGateway gateway) {
    this.gateway = gateway;
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
  public boolean tryWriteResponse(final int requestStreamId, final long requestId) {
    if (rejectionType != RejectionType.NULL_VAL) {
      final Status rejectionResponse = createRejectionResponse();
      gateway.errorCallback(requestId, rejectionResponse);
      return true;
    }

    try {
      gateway.responseCallback(requestId);
      return true;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
