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
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivatedJob;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CancelProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CancelProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CompleteJobRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CompleteJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceWithResultRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceWithResultResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DecisionMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DecisionRequirementsMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployProcessRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployProcessResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceResponse.Builder;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ModifyProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ModifyProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ProcessMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.PublishMessageRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.PublishMessageResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.SetVariablesRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.SetVariablesResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ThrowErrorRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ThrowErrorResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobRetriesRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobRetriesResponse;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentRecord;
import io.camunda.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobBatchRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceResultRecord;
import io.camunda.zeebe.protocol.impl.record.value.variable.VariableDocumentRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;

class GrpcResponseMapper {

  private DirectBuffer valueBufferView;
  private long key;
  private Intent intent;

  private final Map<Class<? extends GeneratedMessageV3>, Callable<GeneratedMessageV3>> mappers =
      Map.ofEntries(
          Map.entry(ActivateJobsRequest.class, this::createJobBatchResponse),
          Map.entry(CancelProcessInstanceRequest.class, this::createCancelInstanceResponse),
          Map.entry(CompleteJobRequest.class, this::createCompleteJobResponse),
          Map.entry(CreateProcessInstanceRequest.class, this::createProcessInstanceResponse),
          Map.entry(
              CreateProcessInstanceWithResultRequest.class,
              this::createProcessInstanceWithResultResponse),
          Map.entry(DeployProcessRequest.class, this::createDeployResponse),
          Map.entry(DeployResourceRequest.class, this::createDeployResourceResponse),
          Map.entry(FailJobRequest.class, this::createFailJobResponse),
          Map.entry(ThrowErrorRequest.class, this::createJobThrowErrorResponse),
          Map.entry(PublishMessageRequest.class, this::createMessageResponse),
          Map.entry(ResolveIncidentRequest.class, this::createResolveIncidentResponse),
          Map.entry(SetVariablesRequest.class, this::createSetVariablesResponse),
          Map.entry(UpdateJobRetriesRequest.class, this::createJobUpdateRetriesResponse),
          Map.entry(ModifyProcessInstanceRequest.class, this::createModifyProcessInstanceResponse));

  GeneratedMessageV3 map(
      final Class<? extends GeneratedMessageV3> requestType,
      final DirectBuffer valueBufferView,
      final long key,
      final Intent intent) {
    try {
      this.valueBufferView = valueBufferView;
      this.key = key;
      this.intent = intent;
      return mappers.get(requestType).call();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated(since = "8.0.0")
  private DeployProcessResponse createDeployResponse() {
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

  private GeneratedMessageV3 createDeployResourceResponse() {
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

  private GeneratedMessageV3 createProcessInstanceResponse() {
    final ProcessInstanceCreationRecord processInstance = new ProcessInstanceCreationRecord();
    processInstance.wrap(valueBufferView);

    return CreateProcessInstanceResponse.newBuilder()
        .setProcessInstanceKey(processInstance.getProcessInstanceKey())
        .setProcessDefinitionKey(processInstance.getProcessDefinitionKey())
        .setBpmnProcessId(processInstance.getBpmnProcessId())
        .setVersion(processInstance.getVersion())
        .build();
  }

  private GeneratedMessageV3 createProcessInstanceWithResultResponse() {
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

  private GeneratedMessageV3 createCancelInstanceResponse() {
    return CancelProcessInstanceResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createModifyProcessInstanceResponse() {
    return ModifyProcessInstanceResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createResolveIncidentResponse() {
    final IncidentRecord incident = new IncidentRecord();
    incident.wrap(valueBufferView);

    return ResolveIncidentResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createSetVariablesResponse() {
    final VariableDocumentRecord variableDocumentRecord = new VariableDocumentRecord();
    variableDocumentRecord.wrap(valueBufferView);

    return SetVariablesResponse.newBuilder().setKey(key).build();
  }

  private GeneratedMessageV3 createMessageResponse() {
    return PublishMessageResponse.newBuilder().setKey(key).build();
  }

  private GeneratedMessageV3 createJobBatchResponse() {
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

  private GeneratedMessageV3 createCompleteJobResponse() {
    return CompleteJobResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createFailJobResponse() {
    return FailJobResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createJobThrowErrorResponse() {
    return ThrowErrorResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createJobUpdateRetriesResponse() {
    return UpdateJobRetriesResponse.newBuilder().build();
  }

  private GeneratedMessageV3 createJobResponse() {
    return switch ((JobIntent) intent) {
      case COMPLETED -> createCompleteJobResponse();
      case FAILED -> createFailJobResponse();
      case ERROR_THROWN -> createJobThrowErrorResponse();
      case RETRIES_UPDATED -> createJobUpdateRetriesResponse();
      default -> throw new UnsupportedOperationException(
          String.format("Job command '%s' is not supported", intent));
    };
  }

  Status createRejectionResponse(
      final RejectionType rejectionType, final Intent intent, final String rejectionReason) {
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
}
