/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Status;
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
import io.camunda.zeebe.logstreams.log.LogStreamRecordWriter;
import io.camunda.zeebe.msgpack.value.ValueArray;
import io.camunda.zeebe.process.test.engine.GrpcResponseWriter.GrpcResponseMapper;
import io.camunda.zeebe.protocol.impl.encoding.MsgPackConverter;
import io.camunda.zeebe.protocol.impl.record.RecordMetadata;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentRecord;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DeploymentResource;
import io.camunda.zeebe.protocol.impl.record.value.incident.IncidentRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobBatchRecord;
import io.camunda.zeebe.protocol.impl.record.value.job.JobRecord;
import io.camunda.zeebe.protocol.impl.record.value.message.MessageRecord;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceCreationRecord;
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
import io.camunda.zeebe.protocol.record.intent.VariableDocumentIntent;
import io.camunda.zeebe.protocol.record.value.VariableDocumentUpdateSemantic;
import io.camunda.zeebe.util.VersionUtil;
import io.camunda.zeebe.util.buffer.BufferUtil;
import io.camunda.zeebe.util.buffer.BufferWriter;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class GrpcToLogStreamGateway extends GatewayGrpc.GatewayImplBase implements AutoCloseable {

  private final LogStreamRecordWriter writer;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Map<Long, ResponseSender> responseSenderMap = new HashMap<>();
  private final RecordMetadata recordMetadata = new RecordMetadata();
  private final AtomicLong requestIdGenerator = new AtomicLong();
  private final int partitionId;
  private final int partitionCount;
  private final int port;

  public GrpcToLogStreamGateway(
      final LogStreamRecordWriter writer,
      final int partitionId,
      final int partitionCount,
      final int port) {
    this.writer = writer;
    this.partitionId = partitionId;
    this.partitionCount = partitionCount;
    this.port = port;
  }

  private void writeCommandWithKey(
      final Long key, final RecordMetadata metadata, final BufferWriter bufferWriter) {
    writer.reset();

    writer.key(key).metadataWriter(metadata).valueWriter(bufferWriter).tryWrite();
  }

  private void writeCommandWithoutKey(
      final RecordMetadata metadata, final BufferWriter bufferWriter) {
    writer.reset();

    writer.keyNull().metadataWriter(metadata).valueWriter(bufferWriter).tryWrite();
  }

  @Override
  public void activateJobs(
      final ActivateJobsRequest request,
      final StreamObserver<ActivateJobsResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createJobBatchResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.JOB_BATCH)
              .intent(JobBatchIntent.ACTIVATE);

          final JobBatchRecord jobBatchRecord = new JobBatchRecord();

          jobBatchRecord.setType(request.getType());
          jobBatchRecord.setWorker(request.getWorker());
          jobBatchRecord.setTimeout(request.getTimeout());
          jobBatchRecord.setMaxJobsToActivate(request.getMaxJobsToActivate());

          writeCommandWithoutKey(recordMetadata, jobBatchRecord);
        });
  }

  @Override
  public void cancelProcessInstance(
      final CancelProcessInstanceRequest request,
      final StreamObserver<CancelProcessInstanceResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(
                  responseObserver, GrpcResponseWriter::createCancelInstanceResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.PROCESS_INSTANCE)
              .intent(ProcessInstanceIntent.CANCEL);

          final ProcessInstanceRecord processInstanceRecord = new ProcessInstanceRecord();
          processInstanceRecord.setProcessInstanceKey(request.getProcessInstanceKey());

          writeCommandWithKey(
              request.getProcessInstanceKey(), recordMetadata, processInstanceRecord);
        });
  }

  @Override
  public void completeJob(
      final CompleteJobRequest request,
      final StreamObserver<CompleteJobResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createCompleteJobResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.JOB)
              .intent(JobIntent.COMPLETE);

          final JobRecord jobRecord = new JobRecord();

          final String variables = request.getVariables();
          if (!variables.isEmpty()) {
            jobRecord.setVariables(
                BufferUtil.wrapArray(MsgPackConverter.convertToMsgPack(variables)));
          }

          writeCommandWithKey(request.getJobKey(), recordMetadata, jobRecord);
        });
  }

  @Override
  public void createProcessInstance(
      final CreateProcessInstanceRequest request,
      final StreamObserver<CreateProcessInstanceResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(
                  responseObserver, GrpcResponseWriter::createProcessInstanceResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.PROCESS_INSTANCE_CREATION)
              .intent(ProcessInstanceCreationIntent.CREATE);

          final ProcessInstanceCreationRecord processInstanceCreationRecord =
              createProcessInstanceCreationRecord(request);
          writeCommandWithoutKey(recordMetadata, processInstanceCreationRecord);
        });
  }

  @Override
  public void createProcessInstanceWithResult(
      final CreateProcessInstanceWithResultRequest request,
      final StreamObserver<CreateProcessInstanceWithResultResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(
                  responseObserver, GrpcResponseWriter::createProcessInstanceWithResultResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.PROCESS_INSTANCE_CREATION)
              .intent(ProcessInstanceCreationIntent.CREATE_WITH_AWAITING_RESULT);

          final ProcessInstanceCreationRecord processInstanceCreationRecord =
              createProcessInstanceCreationRecord(request.getRequest());
          processInstanceCreationRecord.setFetchVariables(request.getFetchVariablesList());

          writeCommandWithoutKey(recordMetadata, processInstanceCreationRecord);
        });
  }

  @Override
  public void deployProcess(
      final DeployProcessRequest request,
      final StreamObserver<DeployProcessResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createDeployResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.DEPLOYMENT)
              .intent(DeploymentIntent.CREATE);

          final DeploymentRecord deploymentRecord = new DeploymentRecord();
          final ValueArray<DeploymentResource> resources = deploymentRecord.resources();

          request
              .getProcessesList()
              .forEach(
                  (processRequestObject -> {
                    resources
                        .add()
                        .setResourceName(processRequestObject.getName())
                        .setResource(processRequestObject.getDefinition().toByteArray());
                  }));

          writeCommandWithoutKey(recordMetadata, deploymentRecord);
        });
  }

  @Override
  public void deployResource(
      final DeployResourceRequest request,
      final StreamObserver<DeployResourceResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(
                  responseObserver, GrpcResponseWriter::createDeployResourceResponse);

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

          writeCommandWithoutKey(recordMetadata, deploymentRecord);
        });
  }

  @Override
  public void failJob(
      final FailJobRequest request, final StreamObserver<FailJobResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createFailJobResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.JOB)
              .intent(JobIntent.FAIL);

          final JobRecord jobRecord = new JobRecord();

          jobRecord.setRetries(request.getRetries());
          jobRecord.setErrorMessage(request.getErrorMessage());

          writeCommandWithKey(request.getJobKey(), recordMetadata, jobRecord);
        });
  }

  @Override
  public void throwError(
      final ThrowErrorRequest request, final StreamObserver<ThrowErrorResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createJobThrowErrorResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.JOB)
              .intent(JobIntent.THROW_ERROR);

          final JobRecord jobRecord = new JobRecord();

          jobRecord.setErrorCode(BufferUtil.wrapString(request.getErrorCode()));
          jobRecord.setErrorMessage(request.getErrorMessage());

          writeCommandWithKey(request.getJobKey(), recordMetadata, jobRecord);
        });
  }

  @Override
  public void publishMessage(
      final PublishMessageRequest request,
      final StreamObserver<PublishMessageResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createMessageResponse);

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

          writeCommandWithoutKey(recordMetadata, messageRecord);
        });
  }

  @Override
  public void resolveIncident(
      final ResolveIncidentRequest request,
      final StreamObserver<ResolveIncidentResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(
                  responseObserver, GrpcResponseWriter::createResolveIncidentResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.INCIDENT)
              .intent(IncidentIntent.RESOLVE);

          final IncidentRecord incidentRecord = new IncidentRecord();

          writeCommandWithKey(request.getIncidentKey(), recordMetadata, incidentRecord);
        });
  }

  @Override
  public void setVariables(
      final SetVariablesRequest request,
      final StreamObserver<SetVariablesResponse> responseObserver) {
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(responseObserver, GrpcResponseWriter::createSetVariablesResponse);

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

          writeCommandWithoutKey(recordMetadata, variableDocumentRecord);
        });
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
    executor.submit(
        () -> {
          final Long requestId =
              registerNewRequest(
                  responseObserver, GrpcResponseWriter::createJobUpdateRetriesResponse);

          prepareRecordMetadata()
              .requestId(requestId)
              .valueType(ValueType.JOB)
              .intent(JobIntent.UPDATE_RETRIES);

          final JobRecord jobRecord = new JobRecord();
          jobRecord.setRetries(request.getRetries());

          writeCommandWithKey(request.getJobKey(), recordMetadata, jobRecord);
        });
  }

  @Override
  public void close() {
    try {
      executor.shutdownNow();
      executor.awaitTermination(60, TimeUnit.SECONDS);
    } catch (final InterruptedException ie) {
      // TODO handle
    }
  }

  private RecordMetadata prepareRecordMetadata() {
    return recordMetadata.reset().recordType(RecordType.COMMAND).requestStreamId(partitionId);
  }

  private <GrpcResponseType extends GeneratedMessageV3> Long registerNewRequest(
      final StreamObserver<?> responseObserver,
      final GrpcResponseMapper<GrpcResponseType> responseMapper) {
    final long currentRequestId = requestIdGenerator.incrementAndGet();
    responseSenderMap.put(currentRequestId, new ResponseSender(responseObserver, responseMapper));
    return currentRequestId;
  }

  private ProcessInstanceCreationRecord createProcessInstanceCreationRecord(
      final GatewayOuterClass.CreateProcessInstanceRequest request) {
    final ProcessInstanceCreationRecord processInstanceCreationRecord =
        new ProcessInstanceCreationRecord();

    processInstanceCreationRecord.setBpmnProcessId(request.getBpmnProcessId());
    processInstanceCreationRecord.setVersion(request.getVersion());
    processInstanceCreationRecord.setProcessDefinitionKey(request.getProcessDefinitionKey());

    final String variables = request.getVariables();
    if (!variables.isEmpty()) {
      processInstanceCreationRecord.setVariables(
          BufferUtil.wrapArray(MsgPackConverter.convertToMsgPack(variables)));
    }
    return processInstanceCreationRecord;
  }

  public void responseCallback(final Long requestId) {
    executor.submit(
        () -> {
          final ResponseSender responseSender = responseSenderMap.remove(requestId);
          responseSender.sendResponse();
        });
  }

  public void errorCallback(final Long requestId, final Status error) {
    executor.submit(
        () -> {
          final ResponseSender responseSender = responseSenderMap.remove(requestId);
          responseSender.sendError(error);
        });
  }

  public String getAddress() {
    return "0.0.0.0:" + port;
  }

  private record ResponseSender(StreamObserver<?> responseObserver, GrpcResponseMapper<? extends GeneratedMessageV3> responseMapper) {

    void sendResponse() {
      final GeneratedMessageV3 response = responseMapper.apply();
      final StreamObserver<GeneratedMessageV3> streamObserver =
          (StreamObserver<GeneratedMessageV3>) responseObserver;
      streamObserver.onNext(response);
      streamObserver.onCompleted();
    }

    void sendError(final Status error) {
      final StreamObserver<GeneratedMessageV3> streamObserver =
          (StreamObserver<GeneratedMessageV3>) responseObserver;
      streamObserver.onError(StatusProto.toStatusException(error));
    }
  }
}
