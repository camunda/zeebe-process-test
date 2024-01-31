/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import io.camunda.zeebe.gateway.protocol.GatewayGrpc;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivateJobsResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.BrokerInfo;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CancelProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CancelProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CompleteJobRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CompleteJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceWithResultRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.CreateProcessInstanceWithResultResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployProcessRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployProcessResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.DeployResourceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.FailJobResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ModifyProcessInstanceRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ModifyProcessInstanceResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.Partition;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.PublishMessageRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.PublishMessageResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ResolveIncidentResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.SetVariablesRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.SetVariablesResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ThrowErrorRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ThrowErrorResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.TopologyRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.TopologyResponse;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobRetriesRequest;
import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.UpdateJobRetriesResponse;
import io.camunda.zeebe.msgpack.value.StringValue;
import io.camunda.zeebe.msgpack.value.ValueArray;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentRecord;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentResource;
import io.camunda.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobBatchRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.impl.record.value.message.MessageRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationStartInstruction;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceModificationActivateInstruction;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceModificationRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceModificationTerminateInstruction;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceModificationVariableInstruction;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord;
import io.camunda.zeebe.protocol.impl.record.value.variable.VariableDocumentRecord;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.intent.IncidentIntent;
import io.camunda.zeebe.protocol.record.intent.JobBatchIntent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceCreationIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceModificationIntent;
import io.camunda.zeebe.protocol.record.intent.VariableDocumentIntent;
import io.camunda.zeebe.protocol.record.value.VariableDocumentUpdateSemantic;
import io.camunda.zeebe.util.VersionUtil;
import io.camunda.zeebe.util.buffer.BufferUtil;
import io.grpc.stub.StreamObserver;
import java.util.List;

class GrpcToLogStreamGateway extends GatewayGrpc.GatewayImplBase {

  private final CommandWriter writer;
  private final int partitionId;
  private final int partitionCount;
  private final int port;
  private final GatewayRequestStore gatewayRequestStore;

  public GrpcToLogStreamGateway(
      final CommandWriter writer,
      final int partitionId,
      final int partitionCount,
      final int port,
      final GatewayRequestStore gatewayRequestStore) {
    this.writer = writer;
    this.partitionId = partitionId;
    this.partitionCount = partitionCount;
    this.port = port;
    this.gatewayRequestStore = gatewayRequestStore;
  }

  @Override
  public void activateJobs(
      final ActivateJobsRequest request,
      final StreamObserver<ActivateJobsResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.JOB_BATCH)
            .intent(JobBatchIntent.ACTIVATE);

    final JobBatchRecord jobBatchRecord = new JobBatchRecord();

    jobBatchRecord.setType(request.getType());
    jobBatchRecord.setWorker(request.getWorker());
    jobBatchRecord.setTimeout(request.getTimeout());
    jobBatchRecord.setMaxJobsToActivate(request.getMaxJobsToActivate());
    setJobBatchRecordVariables(jobBatchRecord, request.getFetchVariableList());

    writer.writeCommandWithoutKey(jobBatchRecord, recordMetadata);
  }

  @Override
  public void cancelProcessInstance(
      final CancelProcessInstanceRequest request,
      final StreamObserver<CancelProcessInstanceResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.PROCESS_INSTANCE)
            .intent(ProcessInstanceIntent.CANCEL);

    final ProcessInstanceRecord processInstanceRecord = new ProcessInstanceRecord();
    processInstanceRecord.setProcessInstanceKey(request.getProcessInstanceKey());

    writer.writeCommandWithKey(
        request.getProcessInstanceKey(), processInstanceRecord, recordMetadata);
  }

  @Override
  public void completeJob(
      final CompleteJobRequest request,
      final StreamObserver<CompleteJobResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.JOB)
            .intent(JobIntent.COMPLETE);

    final JobRecord jobRecord = new JobRecord();

    final String variables = request.getVariables();
    if (!variables.isEmpty()) {
      jobRecord.setVariables(BufferUtil.wrapArray(MsgPackConverter.convertToMsgPack(variables)));
    }

    writer.writeCommandWithKey(request.getJobKey(), jobRecord, recordMetadata);
  }

  @Override
  public void createProcessInstance(
      final CreateProcessInstanceRequest request,
      final StreamObserver<CreateProcessInstanceResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.PROCESS_INSTANCE_CREATION)
            .intent(ProcessInstanceCreationIntent.CREATE);

    final ProcessInstanceCreationRecord processInstanceCreationRecord =
        createProcessInstanceCreationRecord(request);
    writer.writeCommandWithoutKey(processInstanceCreationRecord, recordMetadata);
  }

  @Override
  public void createProcessInstanceWithResult(
      final CreateProcessInstanceWithResultRequest request,
      final StreamObserver<CreateProcessInstanceWithResultResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.PROCESS_INSTANCE_CREATION)
            .intent(ProcessInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT);

    final ProcessInstanceCreationRecord processInstanceCreationRecord =
        createProcessInstanceCreationRecord(request.getRequest());
    processInstanceCreationRecord.setFetchVariables(request.getFetchVariablesList());

    writer.writeCommandWithoutKey(processInstanceCreationRecord, recordMetadata);
  }

  @Override
  public void deployProcess(
      final DeployProcessRequest request,
      final StreamObserver<DeployProcessResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.DEPLOYMENT)
            .intent(DeploymentIntent.CREATE);

    final DeploymentRecord deploymentRecord = new DeploymentRecord();
    final ValueArray<DeploymentResource> resources = deploymentRecord.resources();

    request
        .getProcessesList()
        .forEach(
            (processRequestObject ->
                resources
                    .add()
                    .setResourceName(processRequestObject.getName())
                    .setResource(processRequestObject.getDefinition().toByteArray())));

    writer.writeCommandWithoutKey(deploymentRecord, recordMetadata);
  }

  @Override
  public void deployResource(
      final DeployResourceRequest request,
      final StreamObserver<DeployResourceResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.DEPLOYMENT)
            .intent(DeploymentIntent.CREATE);

    final DeploymentRecord deploymentRecord = new DeploymentRecord();
    final ValueArray<DeploymentResource> resources = deploymentRecord.resources();

    request
        .getResourcesList()
        .forEach(
            (resource ->
                resources
                    .add()
                    .setResourceName(resource.getName())
                    .setResource(resource.getContent().toByteArray())));

    writer.writeCommandWithoutKey(deploymentRecord, recordMetadata);
  }

  @Override
  public void failJob(
      final FailJobRequest request, final StreamObserver<FailJobResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.JOB)
            .intent(JobIntent.FAIL);

    final JobRecord jobRecord = new JobRecord();

    jobRecord.setRetries(request.getRetries());
    jobRecord.setErrorMessage(request.getErrorMessage());
    jobRecord.setRetryBackoff(request.getRetryBackOff());

    writer.writeCommandWithKey(request.getJobKey(), jobRecord, recordMetadata);
  }

  @Override
  public void throwError(
      final ThrowErrorRequest request, final StreamObserver<ThrowErrorResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.JOB)
            .intent(JobIntent.THROW_ERROR);

    final JobRecord jobRecord = new JobRecord();

    jobRecord.setErrorCode(BufferUtil.wrapString(request.getErrorCode()));
    jobRecord.setErrorMessage(request.getErrorMessage());

    writer.writeCommandWithKey(request.getJobKey(), jobRecord, recordMetadata);
  }

  @Override
  public void publishMessage(
      final PublishMessageRequest request,
      final StreamObserver<PublishMessageResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.MESSAGE)
            .intent(MessageIntent.PUBLISH);

    final MessageRecord messageRecord = new MessageRecord();

    messageRecord.setCorrelationKey(request.getCorrelationKey());
    messageRecord.setMessageId(request.getMessageId());
    messageRecord.setName(request.getName());
    messageRecord.setTimeToLive(request.getTimeToLive());
    final String variables = request.getVariables();
    if (!variables.isEmpty()) {
      messageRecord.setVariables(
          BufferUtil.wrapArray(MsgPackConverter.convertToMsgPack(variables)));
    }

    writer.writeCommandWithoutKey(messageRecord, recordMetadata);
  }

  @Override
  public void resolveIncident(
      final ResolveIncidentRequest request,
      final StreamObserver<ResolveIncidentResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.INCIDENT)
            .intent(IncidentIntent.RESOLVE);

    final IncidentRecord incidentRecord = new IncidentRecord();

    writer.writeCommandWithKey(request.getIncidentKey(), incidentRecord, recordMetadata);
  }

  @Override
  public void setVariables(
      final SetVariablesRequest request,
      final StreamObserver<SetVariablesResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.VARIABLE_DOCUMENT)
            .intent(VariableDocumentIntent.UPDATE);

    final VariableDocumentRecord variableDocumentRecord = new VariableDocumentRecord();

    final String variables = request.getVariables();
    if (!variables.isEmpty()) {
      variableDocumentRecord.setVariables(
          BufferUtil.wrapArray(MsgPackConverter.convertToMsgPack(variables)));
    }

    variableDocumentRecord.setScopeKey(request.getElementInstanceKey());
    variableDocumentRecord.setUpdateSemantics(
        request.getLocal()
            ? VariableDocumentUpdateSemantic.LOCAL
            : VariableDocumentUpdateSemantic.PROPAGATE);

    writer.writeCommandWithoutKey(variableDocumentRecord, recordMetadata);
  }

  @Override
  public void topology(
      final TopologyRequest request, final StreamObserver<TopologyResponse> responseObserver) {
    final Partition partition =
        GatewayOuterClass.Partition.newBuilder()
            .setHealth(GatewayOuterClass.Partition.PartitionBrokerHealth.HEALTHY)
            .setRole(GatewayOuterClass.Partition.PartitionBrokerRole.LEADER)
            .setPartitionId(partitionId)
            .build();

    final BrokerInfo brokerInfo =
        GatewayOuterClass.BrokerInfo.newBuilder()
            .addPartitions(partition)
            .setHost("0.0.0.0")
            .setPort(port)
            .setVersion(VersionUtil.getVersion())
            .build();

    final TopologyResponse topologyResponse =
        GatewayOuterClass.TopologyResponse.newBuilder()
            .addBrokers(brokerInfo)
            .setClusterSize(1)
            .setPartitionsCount(partitionCount)
            .setReplicationFactor(1)
            .setGatewayVersion(VersionUtil.getVersion())
            .build();

    responseObserver.onNext(topologyResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void updateJobRetries(
      final UpdateJobRetriesRequest request,
      final StreamObserver<UpdateJobRetriesResponse> responseObserver) {
    final Long requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final RecordMetadata recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.JOB)
            .intent(JobIntent.UPDATE_RETRIES);

    final JobRecord jobRecord = new JobRecord();
    jobRecord.setRetries(request.getRetries());

    writer.writeCommandWithKey(request.getJobKey(), jobRecord, recordMetadata);
  }

  @Override
  public void modifyProcessInstance(
      final ModifyProcessInstanceRequest request,
      final StreamObserver<ModifyProcessInstanceResponse> responseObserver) {
    final var requestId =
        gatewayRequestStore.registerNewRequest(request.getClass(), responseObserver);

    final var recordMetadata =
        prepareRecordMetadata()
            .requestId(requestId)
            .valueType(ValueType.PROCESS_INSTANCE_MODIFICATION)
            .intent(ProcessInstanceModificationIntent.MODIFY);

    final ProcessInstanceModificationRecord record =
        createProcessInstanceModificationRecord(request);

    writer.writeCommandWithKey(request.getProcessInstanceKey(), record, recordMetadata);
  }

  private void setJobBatchRecordVariables(
      final JobBatchRecord jobBatchRecord, final List<String> fetchVariables) {
    final ValueArray<StringValue> variables = jobBatchRecord.variables();
    fetchVariables.stream()
        .map(BufferUtil::wrapString)
        .forEach(buffer -> variables.add().wrap(buffer));
  }

  private ProcessInstanceModificationRecord createProcessInstanceModificationRecord(
      final ModifyProcessInstanceRequest request) {
    final var record = new ProcessInstanceModificationRecord();
    record.setProcessInstanceKey(request.getProcessInstanceKey());
    for (final var activate : request.getActivateInstructionsList()) {
      final var instruction =
          new ProcessInstanceModificationActivateInstruction()
              .setElementId(activate.getElementId())
              .setAncestorScopeKey(activate.getAncestorElementInstanceKey());
      for (final var variable : activate.getVariableInstructionsList()) {
        instruction.addVariableInstruction(
            new ProcessInstanceModificationVariableInstruction()
                .setElementId(variable.getScopeId())
                .setVariables(
                    BufferUtil.wrapArray(
                        MsgPackConverter.convertToMsgPack(variable.getVariables()))));
      }

      record.addActivateInstruction(instruction);
    }

    for (final var terminate : request.getTerminateInstructionsList()) {
      final var instruction =
          new ProcessInstanceModificationTerminateInstruction()
              .setElementInstanceKey(terminate.getElementInstanceKey());
      record.addTerminateInstruction(instruction);
    }
    return record;
  }

  private RecordMetadata prepareRecordMetadata() {
    return new RecordMetadata().recordType(RecordType.COMMAND).requestStreamId(partitionId);
  }

  private ProcessInstanceCreationRecord createProcessInstanceCreationRecord(
      final GatewayOuterClass.CreateProcessInstanceRequest request) {
    final ProcessInstanceCreationRecord processInstanceCreationRecord =
        new ProcessInstanceCreationRecord();

    processInstanceCreationRecord.setBpmnProcessId(request.getBpmnProcessId());
    processInstanceCreationRecord.setVersion(request.getVersion());
    processInstanceCreationRecord.setProcessDefinitionKey(request.getProcessDefinitionKey());

    request.getStartInstructionsList().stream()
        .map(
            startInstruction ->
                new ProcessInstanceCreationStartInstruction()
                    .setElementId(startInstruction.getElementId()))
        .forEach(processInstanceCreationRecord::addStartInstruction);

    final String variables = request.getVariables();
    if (!variables.isEmpty()) {
      processInstanceCreationRecord.setVariables(
          BufferUtil.wrapArray(MsgPackConverter.convertToMsgPack(variables)));
    }
    return processInstanceCreationRecord;
  }

  public String getAddress() {
    return "0.0.0.0:" + port;
  }
}
