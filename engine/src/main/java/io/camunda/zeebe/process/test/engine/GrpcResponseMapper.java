/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivatedJob;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.BroadcastSignalRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.BroadcastSignalResponse;
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
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.EvaluateDecisionRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.EvaluateDecisionResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.EvaluatedDecision;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.EvaluatedDecisionInput;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.EvaluatedDecisionOutput;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FormMetadata;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.MatchedDecisionRule;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.MigrateProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.MigrateProcessInstanceResponse;
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
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobTimeoutRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobTimeoutResponse;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.impl.record.value.decision.DecisionEvaluationRecord;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentRecord;
import io.camunda.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobBatchRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceResultRecord;
import io.camunda.zeebe.protocol.impl.record.value.signal.SignalRecord;
import io.camunda.zeebe.protocol.impl.record.value.variable.VariableDocumentRecord;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;

class GrpcResponseMapper {

  private DirectBuffer valueBufferView;
  private long key;
  private Intent intent;

  private final Map<Class<? extends GeneratedMessage>, Callable<GeneratedMessage>> mappers =
      Map.ofEntries(
          Map.entry(ActivateJobsRequest.class, this::createJobBatchResponse),
          Map.entry(CancelProcessInstanceRequest.class, this::createCancelInstanceResponse),
          Map.entry(CompleteJobRequest.class, this::createCompleteJobResponse),
          Map.entry(CreateProcessInstanceRequest.class, this::createProcessInstanceResponse),
          Map.entry(
              CreateProcessInstanceWithResultRequest.class,
              this::createProcessInstanceWithResultResponse),
          Map.entry(EvaluateDecisionRequest.class, this::evaluateDecisionResponse),
          Map.entry(DeployProcessRequest.class, this::createDeployResponse),
          Map.entry(DeployResourceRequest.class, this::createDeployResourceResponse),
          Map.entry(FailJobRequest.class, this::createFailJobResponse),
          Map.entry(ThrowErrorRequest.class, this::createJobThrowErrorResponse),
          Map.entry(PublishMessageRequest.class, this::createMessageResponse),
          Map.entry(ResolveIncidentRequest.class, this::createResolveIncidentResponse),
          Map.entry(SetVariablesRequest.class, this::createSetVariablesResponse),
          Map.entry(UpdateJobRetriesRequest.class, this::createJobUpdateRetriesResponse),
          Map.entry(UpdateJobTimeoutRequest.class, this::createJobUpdateTimeOutResponse),
          Map.entry(ModifyProcessInstanceRequest.class, this::createModifyProcessInstanceResponse),
          Map.entry(
              MigrateProcessInstanceRequest.class, this::createMigrateProcessInstanceResponse),
          Map.entry(BroadcastSignalRequest.class, this::createBroadcastSignalResponse));

  GeneratedMessage map(
      final Class<? extends GeneratedMessage> requestType,
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

  private GeneratedMessage createDeployResourceResponse() {
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

    deployment.formMetadata().stream()
        .map(
            metadata ->
                FormMetadata.newBuilder()
                    .setFormId(metadata.getFormId())
                    .setVersion(metadata.getVersion())
                    .setFormKey(metadata.getFormKey())
                    .setResourceName(metadata.getResourceName())
                    .setTenantId(metadata.getTenantId()))
        .forEach(metadata -> builder.addDeploymentsBuilder().setForm(metadata));

    return builder.build();
  }

  private GeneratedMessage evaluateDecisionResponse() {
    final DecisionEvaluationRecord evaluationRecord = new DecisionEvaluationRecord();
    evaluationRecord.wrap(valueBufferView);

    final List<EvaluatedDecision> evaluatedDecisions =
        evaluationRecord.evaluatedDecisions().stream()
            .map(
                evaluatedDecision ->
                    EvaluatedDecision.newBuilder()
                        .setDecisionId(evaluatedDecision.getDecisionId())
                        .setDecisionKey(evaluatedDecision.getDecisionKey())
                        .setDecisionName(evaluatedDecision.getDecisionName())
                        .setDecisionVersion(evaluatedDecision.getDecisionVersion())
                        .setDecisionType(evaluatedDecision.getDecisionType())
                        .setDecisionOutput(evaluatedDecision.getDecisionOutput())
                        // map evaluated decision inputs
                        .addAllEvaluatedInputs(
                            evaluatedDecision.evaluatedInputs().stream()
                                .map(
                                    evaluatedInput ->
                                        EvaluatedDecisionInput.newBuilder()
                                            .setInputValue(evaluatedInput.getInputValue())
                                            .setInputName(evaluatedInput.getInputName())
                                            .setInputId(evaluatedInput.getInputId())
                                            .build())
                                .collect(Collectors.toList()))
                        // map matched decision rules
                        .addAllMatchedRules(
                            evaluatedDecision.matchedRules().stream()
                                .map(
                                    matchedRule ->
                                        MatchedDecisionRule.newBuilder()
                                            .setRuleIndex(matchedRule.getRuleIndex())
                                            .setRuleId(matchedRule.getRuleId())
                                            // map evaluated decision outputs
                                            .addAllEvaluatedOutputs(
                                                matchedRule.evaluatedOutputs().stream()
                                                    .map(
                                                        evaluatedOutput ->
                                                            EvaluatedDecisionOutput.newBuilder()
                                                                .setOutputValue(
                                                                    evaluatedOutput
                                                                        .getOutputValue())
                                                                .setOutputName(
                                                                    evaluatedOutput.getOutputName())
                                                                .setOutputId(
                                                                    evaluatedOutput.getOutputId())
                                                                .build())
                                                    .collect(Collectors.toList()))
                                            .build())
                                .collect(Collectors.toList()))
                        .build())
            .collect(Collectors.toList());

    return EvaluateDecisionResponse.newBuilder()
        .setDecisionId(evaluationRecord.getDecisionId())
        .setDecisionKey(evaluationRecord.getDecisionKey())
        .setDecisionName(evaluationRecord.getDecisionName())
        .setDecisionVersion(evaluationRecord.getDecisionVersion())
        .setDecisionRequirementsId(evaluationRecord.getDecisionRequirementsId())
        .setDecisionRequirementsKey(evaluationRecord.getDecisionRequirementsKey())
        .setDecisionOutput(evaluationRecord.getDecisionOutput())
        .setFailedDecisionId(evaluationRecord.getFailedDecisionId())
        .setFailureMessage(evaluationRecord.getEvaluationFailureMessage())
        .addAllEvaluatedDecisions(evaluatedDecisions)
        .build();
  }

  private GeneratedMessage createProcessInstanceResponse() {
    final ProcessInstanceCreationRecord processInstance = new ProcessInstanceCreationRecord();
    processInstance.wrap(valueBufferView);

    return CreateProcessInstanceResponse.newBuilder()
        .setProcessInstanceKey(processInstance.getProcessInstanceKey())
        .setProcessDefinitionKey(processInstance.getProcessDefinitionKey())
        .setBpmnProcessId(processInstance.getBpmnProcessId())
        .setVersion(processInstance.getVersion())
        .build();
  }

  private GeneratedMessage createProcessInstanceWithResultResponse() {
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

  private GeneratedMessage createCancelInstanceResponse() {
    return CancelProcessInstanceResponse.newBuilder().build();
  }

  private GeneratedMessage createModifyProcessInstanceResponse() {
    return ModifyProcessInstanceResponse.newBuilder().build();
  }

  private GeneratedMessage createMigrateProcessInstanceResponse() {
    return MigrateProcessInstanceResponse.getDefaultInstance();
  }

  private GeneratedMessage createBroadcastSignalResponse() {
    final SignalRecord signal = new SignalRecord();
    signal.wrap(valueBufferView);

    return BroadcastSignalResponse.newBuilder().setKey(key).build();
  }

  private GeneratedMessage createResolveIncidentResponse() {
    final IncidentRecord incident = new IncidentRecord();
    incident.wrap(valueBufferView);

    return ResolveIncidentResponse.newBuilder().build();
  }

  private GeneratedMessage createSetVariablesResponse() {
    final VariableDocumentRecord variableDocumentRecord = new VariableDocumentRecord();
    variableDocumentRecord.wrap(valueBufferView);

    return SetVariablesResponse.newBuilder().setKey(key).build();
  }

  private GeneratedMessage createMessageResponse() {
    return PublishMessageResponse.newBuilder().setKey(key).build();
  }

  private GeneratedMessage createJobBatchResponse() {
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
                .map((entry) -> mapToActivatedJob(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()))
        .build();
  }

  static ActivatedJob mapToActivatedJob(final long key, final JobRecord job) {
    return ActivatedJob.newBuilder()
        .setKey(key)
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
        .setCustomHeaders(MsgPackConverter.convertToJson(job.getCustomHeadersBuffer()))
        .setVariables(MsgPackConverter.convertToJson(job.getVariablesBuffer()))
        .build();
  }

  private GeneratedMessage createCompleteJobResponse() {
    return CompleteJobResponse.newBuilder().build();
  }

  private GeneratedMessage createFailJobResponse() {
    return FailJobResponse.newBuilder().build();
  }

  private GeneratedMessage createJobThrowErrorResponse() {
    return ThrowErrorResponse.newBuilder().build();
  }

  private GeneratedMessage createJobUpdateRetriesResponse() {
    return UpdateJobRetriesResponse.newBuilder().build();
  }

  private GeneratedMessage createJobUpdateTimeOutResponse() {
    return UpdateJobTimeoutResponse.newBuilder().build();
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
