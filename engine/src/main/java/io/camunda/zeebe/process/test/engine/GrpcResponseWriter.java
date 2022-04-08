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

  @Deprecated(since = "8.0.0")
  protected static DeployProcessResponse createDeployResponse() {
    final DeploymentRecord deployment = new DeploymentRecord();
    deployment.wrap(valueBufferView);

    return DeployProcessResponse.newBuilder()
        .setKey(key)
        .addAllProcesses(
            deployment.getProcessesMetadata().stream()
                .map(
                    metadata ->
                        ProcessMetadata.newBuilder()
                            .setProcessDefinitionKey(metadata.getProcessDefinitionKey())
                            .setBpmnProcessId(metadata.getBpmnProcessId())
                            .setVersion(metadata.getVersion())
                            .setResourceName(metadata.getResourceName())
                            .build())
                .collect(Collectors.toList()))
        .build();
  }

  protected static GeneratedMessageV3 createDeployResourceResponse() {
    final DeploymentRecord deployment = new DeploymentRecord();
    deployment.wrap(valueBufferView);

    final Builder builder = DeployResourceResponse.newBuilder().setKey(key);
    deployment.getProcessesMetadata().stream()
        .map(
            metadata ->
                ProcessMetadata.newBuilder()
                    .setBpmnProcessId(metadata.getBpmnProcessId())
                    .setVersion(metadata.getVersion())
                    .setProcessDefinitionKey(metadata.getProcessDefinitionKey())
                    .setResourceName(metadata.getResourceName())
                    .build())
        .forEach(metadata -> builder.addDeploymentsBuilder().setProcess(metadata));

    deployment.decisionsMetadata().stream()
        .map(
            metadata ->
                DecisionMetadata.newBuilder()
                    .setDmnDecisionId(metadata.getDecisionId())
                    .setDmnDecisionName(metadata.getDecisionName())
                    .setVersion(metadata.getVersion())
                    .setDecisionKey(metadata.getDecisionKey())
                    .setDmnDecisionRequirementsId(metadata.getDecisionRequirementsId())
                    .setDecisionRequirementsKey(metadata.getDecisionRequirementsKey())
                    .build())
        .forEach(metadata -> builder.addDeploymentsBuilder().setDecision(metadata));

    deployment.decisionRequirementsMetadata().stream()
        .map(
            metadata ->
                DecisionRequirementsMetadata.newBuilder()
                    .setDmnDecisionRequirementsId(metadata.getDecisionRequirementsId())
                    .setDmnDecisionRequirementsName(metadata.getDecisionRequirementsName())
                    .setVersion(metadata.getDecisionRequirementsVersion())
                    .setDecisionRequirementsKey(metadata.getDecisionRequirementsKey())
                    .setResourceName(metadata.getResourceName())
                    .build())
        .forEach(metadata -> builder.addDeploymentsBuilder().setDecisionRequirements(metadata));

    return builder.build();
  }

  protected static GeneratedMessageV3 createProcessInstanceResponse() {
    final ProcessInstanceCreationRecord processInstance = new ProcessInstanceCreationRecord();
    processInstance.wrap(valueBufferView);

    return CreateProcessInstanceResponse.newBuilder()
        .setProcessInstanceKey(processInstance.getProcessInstanceKey())
        .setProcessDefinitionKey(processInstance.getProcessDefinitionKey())
        .setBpmnProcessId(processInstance.getBpmnProcessId())
        .setVersion(processInstance.getVersion())
        .build();
  }

  protected static GeneratedMessageV3 createProcessInstanceWithResultResponse() {
    final ProcessInstanceResultRecord processInstanceResult = new ProcessInstanceResultRecord();
    processInstanceResult.wrap(valueBufferView);

    return CreateProcessInstanceWithResultResponse.newBuilder()
        .setProcessInstanceKey(processInstanceResult.getProcessInstanceKey())
        .setProcessDefinitionKey(processInstanceResult.getProcessDefinitionKey())
        .setBpmnProcessId(processInstanceResult.getBpmnProcessId())
        .setVersion(processInstanceResult.getVersion())
        .setVariables(MsgPackConverter.convertToJson(processInstanceResult.getVariablesBuffer()))
        .build();
  }

  protected static GeneratedMessageV3 createCancelInstanceResponse() {
    return CancelProcessInstanceResponse.newBuilder().build();
  }

  protected static GeneratedMessageV3 createResolveIncidentResponse() {
    final IncidentRecord incident = new IncidentRecord();
    incident.wrap(valueBufferView);

    return ResolveIncidentResponse.newBuilder().build();
  }

  protected static GeneratedMessageV3 createSetVariablesResponse() {
    final VariableDocumentRecord variableDocumentRecord = new VariableDocumentRecord();
    variableDocumentRecord.wrap(valueBufferView);

    return SetVariablesResponse.newBuilder().setKey(key).build();
  }

  protected static GeneratedMessageV3 createMessageResponse() {
    return PublishMessageResponse.newBuilder().setKey(key).build();
  }

  protected static GeneratedMessageV3 createJobBatchResponse() {
    final JobBatchRecord jobBatch = new JobBatchRecord();
    jobBatch.wrap(valueBufferView);

    final Map<Long, JobRecord> jobsWithKeys = new HashMap<>();
    for (int index = 0; index < jobBatch.getJobKeys().size(); index++) {
      final Long key = jobBatch.getJobKeys().get(index);
      final JobRecord value = (JobRecord) jobBatch.getJobs().get(index);
      jobsWithKeys.put(key, value);
    }

    return ActivateJobsResponse.newBuilder()
        .addAllJobs(
            jobsWithKeys.entrySet().stream()
                .map(
                    (entry) -> {
                      final JobRecord job = entry.getValue();
                      return ActivatedJob.newBuilder()
                          .setKey(entry.getKey())
                          .setType(job.getType())
                          .setRetries(job.getRetries())
                          .setWorker(job.getWorker())
                          .setDeadline(job.getDeadline())
                          .setProcessDefinitionKey(job.getProcessDefinitionKey())
                          .setBpmnProcessId(job.getBpmnProcessId())
                          .setProcessDefinitionVersion(job.getProcessDefinitionVersion())
                          .setProcessInstanceKey(job.getProcessInstanceKey())
                          .setElementId(job.getElementId())
                          .setElementInstanceKey(job.getElementInstanceKey())
                          .setCustomHeaders(
                              MsgPackConverter.convertToJson(job.getCustomHeadersBuffer()))
                          .setVariables(MsgPackConverter.convertToJson(job.getVariablesBuffer()))
                          .build();
                    })
                .collect(Collectors.toList()))
        .build();
  }

  protected static GeneratedMessageV3 createCompleteJobResponse() {
    return CompleteJobResponse.newBuilder().build();
  }

  protected static GeneratedMessageV3 createFailJobResponse() {
    return FailJobResponse.newBuilder().build();
  }

  protected static GeneratedMessageV3 createJobThrowErrorResponse() {
    return ThrowErrorResponse.newBuilder().build();
  }

  protected static GeneratedMessageV3 createJobUpdateRetriesResponse() {
    return UpdateJobRetriesResponse.newBuilder().build();
  }

  protected static GeneratedMessageV3 createJobResponse() {
    return switch ((JobIntent) intent) {
      case COMPLETED -> createCompleteJobResponse();
      case FAILED -> createFailJobResponse();
      case ERROR_THROWN -> createJobThrowErrorResponse();
      case RETRIES_UPDATED -> createJobUpdateRetriesResponse();
      default -> throw new UnsupportedOperationException(
          String.format("Job command '%s' is not supported", intent));
    };
  }

  private Status createRejectionResponse() {
    final int statusCode =
        switch (rejectionType) {
          case INVALID_ARGUMENT -> Code.INVALID_ARGUMENT_VALUE;
          case NOT_FOUND -> Code.NOT_FOUND_VALUE;
          case ALREADY_EXISTS -> Code.ALREADY_EXISTS_VALUE;
          case INVALID_STATE -> Code.FAILED_PRECONDITION_VALUE;
          case PROCESSING_ERROR -> Code.INTERNAL_VALUE;
          default -> Code.UNKNOWN_VALUE;
        };

    return Status.newBuilder()
        .setMessage(
            String.format(
                "Command '%s' rejected with code '%s': %s", intent, rejectionType, rejectionReason))
        .setCode(statusCode)
        .build();
  }

  @FunctionalInterface
  public interface GrpcResponseMapper<GrpcResponseType extends GeneratedMessageV3> {
    GrpcResponseType apply();
  }
}
