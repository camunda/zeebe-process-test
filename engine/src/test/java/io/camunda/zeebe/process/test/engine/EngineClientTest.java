/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.process.test.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.util.concurrent.Uninterruptibles;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.ClientException;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.BroadcastSignalResponse;
import io.camunda.zeebe.client.api.response.BrokerInfo;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.EvaluateDecisionResponse;
import io.camunda.zeebe.client.api.response.Form;
import io.camunda.zeebe.client.api.response.MigrateProcessInstanceResponse;
import io.camunda.zeebe.client.api.response.PartitionBrokerHealth;
import io.camunda.zeebe.client.api.response.PartitionBrokerRole;
import io.camunda.zeebe.client.api.response.PartitionInfo;
import io.camunda.zeebe.client.api.response.Process;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.client.api.response.SetVariablesResponse;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.filters.JobRecordStreamFilter;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.SignalIntent;
import io.camunda.zeebe.protocol.record.intent.TimerIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.BpmnEventType;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.util.VersionUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PrintRecordStreamExtension.class)
class EngineClientTest {

  private static final String DMN_RESOURCE = "dmn/drg-force-user.dmn";

  private ZeebeTestEngine zeebeEngine;
  private ZeebeClient zeebeClient;
  private List<Intent> responseIntents;

  @BeforeEach
  void setupGrpcServer() {
    responseIntents = new ArrayList<>();
    zeebeEngine = EngineFactory.create(responseIntents::add);
    zeebeEngine.start();
    zeebeClient = zeebeEngine.createClient();
  }

  @AfterEach
  void tearDown() {
    zeebeEngine.stop();
    zeebeClient.close();
  }

  @Test
  void shouldRequestTopology() {
    // given

    // when
    final Topology topology = zeebeClient.newTopologyRequest().send().join();

    // then
    assertThat(topology.getClusterSize()).isEqualTo(1);
    assertThat(topology.getReplicationFactor()).isEqualTo(1);
    assertThat(topology.getPartitionsCount()).isEqualTo(1);
    assertThat(topology.getGatewayVersion()).isEqualTo(VersionUtil.getVersion());

    assertThat(topology.getBrokers()).hasSize(1);
    final BrokerInfo broker = topology.getBrokers().get(0);
    assertThat(broker.getAddress()).isEqualTo(zeebeEngine.getGatewayAddress());
    assertThat(broker.getVersion()).isEqualTo(VersionUtil.getVersion());

    assertThat(broker.getPartitions()).hasSize(1);
    final PartitionInfo partition = broker.getPartitions().get(0);
    assertThat(partition.getHealth()).isEqualTo(PartitionBrokerHealth.HEALTHY);
    assertThat(partition.isLeader()).isTrue();
    assertThat(partition.getRole()).isEqualTo(PartitionBrokerRole.LEADER);
    assertThat(partition.getPartitionId()).isEqualTo(1);
  }

  @Test
  void shouldUseBuiltInClient() {
    // given

    // when
    final Topology topology = zeebeClient.newTopologyRequest().send().join();

    // then
    assertThat(topology).isNotNull();
  }

  @Test
  void shouldUseGatewayAddressToBuildClient() {
    // given
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .applyEnvironmentVariableOverrides(false)
            .usePlaintext()
            .gatewayAddress(zeebeEngine.getGatewayAddress())
            .build();

    // when
    final Topology topology = zeebeClient.newTopologyRequest().send().join();
    client.close();

    // then
    assertThat(topology).isNotNull();
  }

  @Test
  void shouldPublishMessage() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .intermediateCatchEvent()
                .message(message -> message.name("a").zeebeCorrelationKeyExpression("key"))
                .endEvent()
                .done(),
            "process.bpmn")
        .send()
        .join();

    final ZeebeFuture<ProcessInstanceResult> processInstanceResult =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(Map.of("key", "key-1"))
            .withResult()
            .send();

    // when
    zeebeClient
        .newPublishMessageCommand()
        .messageName("a")
        .correlationKey("key-1")
        .variables(Map.of("message", "correlated"))
        .send()
        .join();

    // then
    assertThat(processInstanceResult.join().getVariablesAsMap())
        .containsEntry("message", "correlated");
  }

  @Test
  void shouldDeployProcess() {
    // given

    // when
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
                "simpleProcess.bpmn")
            .send()
            .join();

    // then
    assertThat(deployment.getKey()).isPositive();
    assertThat(deployment.getProcesses()).isNotEmpty();

    final Process process = deployment.getProcesses().get(0);

    assertThat(process.getVersion()).isEqualTo(1);
    assertThat(process.getResourceName()).isEqualTo("simpleProcess.bpmn");
    assertThat(process.getBpmnProcessId()).isEqualTo("simpleProcess");
    assertThat(process.getProcessDefinitionKey()).isPositive();
  }

  @Test
  void shouldDeployResource() {
    // given

    // when
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployResourceCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
                "simpleProcess.bpmn")
            .send()
            .join();

    // then
    assertThat(deployment.getKey()).isPositive();
    assertThat(deployment.getProcesses()).isNotEmpty();

    final Process process = deployment.getProcesses().get(0);

    assertThat(process.getVersion()).isEqualTo(1);
    assertThat(process.getResourceName()).isEqualTo("simpleProcess.bpmn");
    assertThat(process.getBpmnProcessId()).isEqualTo("simpleProcess");
    assertThat(process.getProcessDefinitionKey()).isPositive();
  }

  @Test
  void shouldCreateInstanceWithoutVariables() {
    // given
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
                "simpleProcess.bpmn")
            .send()
            .join();

    // when
    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .send()
            .join();

    // then
    assertThat(processInstance.getProcessInstanceKey()).isPositive();
    assertThat(processInstance.getBpmnProcessId()).isEqualTo("simpleProcess");
    assertThat(processInstance.getProcessDefinitionKey())
        .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
    assertThat(processInstance.getVersion()).isEqualTo(1);
  }

  @Test
  void shouldRejectCommand() {
    // given

    // when
    final ZeebeFuture<ProcessInstanceEvent> future =
        zeebeClient.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send();

    // then
    assertThatThrownBy(future::join)
        .isInstanceOf(ClientException.class)
        .hasMessage(
            "Command 'CREATE' rejected with code 'NOT_FOUND': Expected to find process definition with process ID 'process', but none found");
  }

  @Test
  void shouldCreateProcessInstance() {
    // given
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
                "simpleProcess.bpmn")
            .send()
            .join();

    // when
    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .variables(Map.of("test", 1))
            .send()
            .join();

    // then
    assertThat(processInstance.getProcessInstanceKey()).isPositive();
    assertThat(processInstance.getBpmnProcessId()).isEqualTo("simpleProcess");
    assertThat(processInstance.getProcessDefinitionKey())
        .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
    assertThat(processInstance.getVersion()).isEqualTo(1);
  }

  @Test
  void shouldCancelProcessInstance() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", task -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .variables(Map.of("test", 1))
            .send()
            .join();

    // when - then
    zeebeClient.newCancelInstanceCommand(processInstance.getProcessInstanceKey()).send().join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<ProcessInstanceRecordValue>> first =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .processInstanceRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_TERMINATED)
                      .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
                      .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.UNSPECIFIED)
                      .findFirst();
              assertThat(first).isNotEmpty();
            });
  }

  @Test
  void shouldModifyProcessInstance() throws InterruptedException, TimeoutException {
    // given
    zeebeClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task1", task -> task.zeebeJobType("task1"))
                .serviceTask("task2", task -> task.zeebeJobType("task2"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .send()
            .join();

    zeebeEngine.waitForIdleState(Duration.ofSeconds(1));

    final long task1Key =
        StreamSupport.stream(
                RecordStream.of(zeebeEngine.getRecordStreamSource())
                    .processInstanceRecords()
                    .spliterator(),
                false)
            .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_ACTIVATED)
            .filter(r -> r.getValue().getElementId().equals("task1"))
            .findFirst()
            .get()
            .getKey();

    // when
    zeebeClient
        .newModifyProcessInstanceCommand(processInstance.getProcessInstanceKey())
        .terminateElement(task1Key)
        .and()
        .activateElement("task2")
        .withVariables(Map.of("variable", " value"))
        .send()
        .join();
    zeebeEngine.waitForIdleState(Duration.ofSeconds(1));

    // then
    assertThat(
            StreamSupport.stream(
                    RecordStream.of(zeebeEngine.getRecordStreamSource())
                        .processInstanceRecords()
                        .spliterator(),
                    false)
                .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_TERMINATED)
                .filter(r -> r.getValue().getElementId().equals("task1"))
                .findFirst())
        .isNotEmpty();
    assertThat(
            StreamSupport.stream(
                    RecordStream.of(zeebeEngine.getRecordStreamSource())
                        .processInstanceRecords()
                        .spliterator(),
                    false)
                .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_ACTIVATED)
                .filter(r -> r.getValue().getElementId().equals("task2"))
                .findFirst())
        .isNotEmpty();
  }

  @Test
  void shouldMigrateProcessInstance() throws InterruptedException, TimeoutException {
    // given
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployResourceCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("sourceProcess")
                    .startEvent()
                    .serviceTask("A", b -> b.zeebeJobType("a"))
                    .endEvent()
                    .done(),
                "sourceProcess.bpmn")
            .addProcessModel(
                Bpmn.createExecutableProcess("targetProcess")
                    .startEvent()
                    .serviceTask("B", b -> b.zeebeJobTypeExpression("b"))
                    .endEvent()
                    .done(),
                "targetProcess.bpmn")
            .send()
            .join();

    final long processInstanceKey =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("sourceProcess")
            .latestVersion()
            .send()
            .join()
            .getProcessInstanceKey();

    zeebeEngine.waitForIdleState(Duration.ofSeconds(1));

    final long targetProcessDefinitionKey =
        deployment.getProcesses().stream()
            .filter(p -> p.getBpmnProcessId().equals("targetProcess"))
            .findFirst()
            .orElseThrow()
            .getProcessDefinitionKey();

    final MigrateProcessInstanceResponse response =
        zeebeClient
            .newMigrateProcessInstanceCommand(processInstanceKey)
            .migrationPlan(targetProcessDefinitionKey)
            .addMappingInstruction("A", "B")
            .send()
            .join();

    assertThat(response).isNotNull();
  }

  @Test
  void shouldUpdateVariablesOnProcessInstance() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", task -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .variables(Map.of("test", 1))
            .send()
            .join();

    // when
    final SetVariablesResponse variablesResponse =
        zeebeClient
            .newSetVariablesCommand(processInstance.getProcessInstanceKey())
            .variables(Map.of("test123", 234))
            .local(true)
            .send()
            .join();

    // then
    assertThat(variablesResponse).isNotNull();
    assertThat(variablesResponse.getKey()).isPositive();
  }

  @Test
  void shouldCreateProcessInstanceWithResult() {
    // given
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
                "simpleProcess.bpmn")
            .send()
            .join();

    // when
    final ProcessInstanceResult processInstanceResult =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .variables(Map.of("test", 1))
            .withResult()
            .send()
            .join();

    // then
    assertThat(processInstanceResult.getProcessInstanceKey()).isPositive();
    assertThat(processInstanceResult.getBpmnProcessId()).isEqualTo("simpleProcess");
    assertThat(processInstanceResult.getProcessDefinitionKey())
        .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
    assertThat(processInstanceResult.getVersion()).isEqualTo(1);
    assertThat(processInstanceResult.getVariablesAsMap()).containsEntry("test", 1);
  }

  @Test
  void shouldEvaluateDecisionByDecisionId() {
    // given
    zeebeClient.newDeployResourceCommand().addResourceFromClasspath(DMN_RESOURCE).send().join();
    final String decisionId = "jedi_or_sith";

    // when
    final EvaluateDecisionResponse response =
        zeebeClient
            .newEvaluateDecisionCommand()
            .decisionId(decisionId)
            .variables(Map.of("lightsaberColor", "blue"))
            .send()
            .join();

    // then
    assertThat(response.getFailedDecisionId())
        .describedAs("Expect that a successful result has no failed decision")
        .isEmpty();
    assertThat(response.getFailureMessage())
        .describedAs("Expect that a successful result has no failure message")
        .isEmpty();
    assertThat(response.getDecisionId()).isEqualTo(decisionId);
    assertThat(response.getDecisionVersion()).isOne();
    assertThat(response.getDecisionName()).isEqualTo("Jedi or Sith");
    assertThat(response.getDecisionOutput()).isEqualTo("\"Jedi\"");
  }

  @Test
  void shouldEvaluateDecisionByDecisionKey() {
    // given
    final DeploymentEvent deploymentEvent =
        zeebeClient.newDeployResourceCommand().addResourceFromClasspath(DMN_RESOURCE).send().join();
    final String decisionId = "jedi_or_sith";
    final long decisionKey =
        deploymentEvent.getDecisions().stream()
            .filter(decision -> decision.getDmnDecisionId().equals(decisionId))
            .findFirst()
            .get()
            .getDecisionKey();

    // when
    final EvaluateDecisionResponse response =
        zeebeClient
            .newEvaluateDecisionCommand()
            .decisionKey(decisionKey)
            .variables(Map.of("lightsaberColor", "blue"))
            .send()
            .join();

    // then
    assertThat(response.getFailedDecisionId())
        .describedAs("Expect that a successful result has no failed decision")
        .isEmpty();
    assertThat(response.getFailureMessage())
        .describedAs("Expect that a successful result has no failure message")
        .isEmpty();
    assertThat(response.getDecisionId()).isEqualTo(decisionId);
    assertThat(response.getDecisionVersion()).isOne();
    assertThat(response.getDecisionName()).isEqualTo("Jedi or Sith");
    assertThat(response.getDecisionOutput()).isEqualTo("\"Jedi\"");
  }

  @Test
  void shouldActivateJob() {
    // given
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployCommand()
            .addProcessModel(
                Bpmn.createExecutableProcess("simpleProcess")
                    .startEvent()
                    .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                    .endEvent()
                    .done(),
                "simpleProcess.bpmn")
            .send()
            .join();

    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .variables(Map.of("test", 1))
            .send()
            .join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              // when
              final ActivateJobsResponse activateJobsResponse =
                  zeebeClient
                      .newActivateJobsCommand()
                      .jobType("jobType")
                      .maxJobsToActivate(32)
                      .timeout(Duration.ofMinutes(1))
                      .workerName("worker")
                      .fetchVariables(List.of("test"))
                      .send()
                      .join();

              // then
              final List<ActivatedJob> jobs = activateJobsResponse.getJobs();
              assertThat(jobs).isNotEmpty();
              assertThat(jobs.get(0).getBpmnProcessId()).isEqualTo("simpleProcess");
              assertThat(jobs.get(0).getProcessDefinitionKey())
                  .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
              assertThat(jobs.get(0).getProcessInstanceKey())
                  .isEqualTo(processInstance.getProcessInstanceKey());
              assertThat(jobs.get(0).getRetries()).isEqualTo(3);
              assertThat(jobs.get(0).getType()).isEqualTo("jobType");
              assertThat(jobs.get(0).getWorker()).isEqualTo("worker");
            });
  }

  @Test
  void shouldCompleteJob() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("test"))
                .endEvent()
                .done(),
            "process.bpmn")
        .send()
        .join();

    zeebeClient
        .newWorker()
        .jobType("test")
        .handler(
            (jobClient, job) ->
                jobClient.newCompleteCommand(job.getKey()).variables(Map.of("x", 1)).send().join())
        .open();

    // when
    final ProcessInstanceResult processInstanceResult =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .withResult()
            .send()
            .join();

    // then
    assertThat(processInstanceResult.getVariablesAsMap()).containsEntry("x", 1);
  }

  @Test
  void shouldFailJob() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(Map.of("test", 1))
        .send()
        .join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              final ActivateJobsResponse activateJobsResponse =
                  zeebeClient
                      .newActivateJobsCommand()
                      .jobType("jobType")
                      .maxJobsToActivate(32)
                      .timeout(Duration.ofMinutes(1))
                      .workerName("yolo")
                      .fetchVariables(List.of("test"))
                      .send()
                      .join();

              final List<ActivatedJob> jobs = activateJobsResponse.getJobs();
              assertThat(jobs).isNotEmpty();
              final ActivatedJob job = jobs.get(0);

              // when - then
              zeebeClient
                  .newFailCommand(job.getKey())
                  .retries(0)
                  .errorMessage("This failed oops.")
                  .send()
                  .join();

              Awaitility.await()
                  .untilAsserted(
                      () -> {
                        final Optional<Record<JobRecordValue>> failedJob =
                            StreamSupport.stream(
                                    RecordStream.of(zeebeEngine.getRecordStreamSource())
                                        .jobRecords()
                                        .spliterator(),
                                    false)
                                .filter(r -> r.getKey() == job.getKey())
                                .filter(r -> r.getIntent() == JobIntent.FAILED)
                                .findFirst();

                        assertThat(failedJob).isNotEmpty();
                      });
            });
  }

  @Test
  void shouldThrowErrorOnJob() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                .boundaryEvent("error")
                .error("0xCAFE")
                .zeebeOutputExpression("error_var", "error_var")
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .send()
        .join();

    // when
    Awaitility.await()
        .untilAsserted(
            () -> {
              final ActivateJobsResponse activateJobsResponse =
                  zeebeClient
                      .newActivateJobsCommand()
                      .jobType("jobType")
                      .maxJobsToActivate(32)
                      .timeout(Duration.ofMinutes(1))
                      .workerName("yolo")
                      .send()
                      .join();

              final List<ActivatedJob> jobs = activateJobsResponse.getJobs();
              assertThat(jobs).isNotEmpty();
              final ActivatedJob job = jobs.get(0);

              zeebeClient
                  .newThrowErrorCommand(job.getKey())
                  .errorCode("0xCAFE")
                  .errorMessage("What just happened.")
                  .variable("error_var", "Out of coffee")
                  .send()
                  .join();
            });

    // then
    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<ProcessInstanceRecordValue>> boundaryEvent =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .processInstanceRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED)
                      .filter(
                          r -> r.getValue().getBpmnElementType() == BpmnElementType.BOUNDARY_EVENT)
                      .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.ERROR)
                      .findFirst();

              assertThat(boundaryEvent)
                  .describedAs("Expect that the error boundary event is completed")
                  .isNotEmpty();

              final var errorVariable =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .variableRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getValue().getName().equals("error_var"))
                      .findFirst();

              assertThat(errorVariable)
                  .describedAs("Expect that the error variable is set")
                  .isPresent()
                  .hasValueSatisfying(
                      record ->
                          assertThat(record.getValue().getValue()).isEqualTo("\"Out of coffee\""));
            });
  }

  @Test
  void shouldUpdateRetiresOnJob() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(Map.of("test", 1))
        .send()
        .join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              final ActivateJobsResponse activateJobsResponse =
                  zeebeClient
                      .newActivateJobsCommand()
                      .jobType("jobType")
                      .maxJobsToActivate(32)
                      .timeout(Duration.ofMinutes(1))
                      .workerName("yolo")
                      .fetchVariables(List.of("test"))
                      .send()
                      .join();

              final List<ActivatedJob> jobs = activateJobsResponse.getJobs();
              assertThat(jobs).isNotEmpty();
              final ActivatedJob job = jobs.get(0);

              // when - then
              zeebeClient.newUpdateRetriesCommand(job.getKey()).retries(3).send().join();

              Awaitility.await()
                  .untilAsserted(
                      () -> {
                        final Optional<Record<JobRecordValue>> retriesUpdated =
                            StreamSupport.stream(
                                    RecordStream.of(zeebeEngine.getRecordStreamSource())
                                        .jobRecords()
                                        .spliterator(),
                                    false)
                                .filter(r -> r.getKey() == job.getKey())
                                .filter(r -> r.getIntent() == JobIntent.RETRIES_UPDATED)
                                .findFirst();
                        assertThat(retriesUpdated).isNotEmpty();
                      });
            });
  }

  @Test
  void shouldReturnOnlySpecifiedVariablesOnJobActivation() {
    // given
    zeebeClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(
            Map.ofEntries(
                Map.entry("var_a", "val_a"),
                Map.entry("var_b", "val_b"),
                Map.entry("var_c", "val_c"),
                Map.entry("var_d", "val_d")))
        .send()
        .join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              // when
              final ActivateJobsResponse activateJobsResponse =
                  zeebeClient
                      .newActivateJobsCommand()
                      .jobType("jobType")
                      .maxJobsToActivate(32)
                      .timeout(Duration.ofMinutes(1))
                      .workerName("yolo")
                      .fetchVariables(List.of("var_b", "var_d"))
                      .send()
                      .join();

              // then
              final List<ActivatedJob> jobs = activateJobsResponse.getJobs();
              assertThat(jobs)
                  .first()
                  .extracting(ActivatedJob::getVariablesAsMap)
                  .isEqualTo(Map.of("var_b", "val_b", "var_d", "val_d"));
            });
  }

  @Test
  void shouldUpdateDeadlineOnJob() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(Map.of("test", 1))
        .send()
        .join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              final ActivateJobsResponse activateJobsResponse =
                  zeebeClient
                      .newActivateJobsCommand()
                      .jobType("jobType")
                      .maxJobsToActivate(32)
                      .timeout(Duration.ofMinutes(10))
                      .workerName("yolo")
                      .fetchVariables(List.of("test"))
                      .send()
                      .join();

              final List<ActivatedJob> jobs = activateJobsResponse.getJobs();
              assertThat(jobs).isNotEmpty();
              final ActivatedJob job = jobs.get(0);

              // when - then
              zeebeClient
                  .newUpdateTimeoutCommand(job.getKey())
                  .timeout(Duration.ofMinutes(11))
                  .send()
                  .join();

              Awaitility.await()
                  .untilAsserted(
                      () -> {
                        final Optional<Record<JobRecordValue>> timeoutUpdated =
                            StreamSupport.stream(
                                    RecordStream.of(zeebeEngine.getRecordStreamSource())
                                        .jobRecords()
                                        .spliterator(),
                                    false)
                                .filter(r -> r.getKey() == job.getKey())
                                .filter(r -> r.getIntent() == JobIntent.TIMEOUT_UPDATED)
                                .findFirst();
                        assertThat(timeoutUpdated).isNotEmpty();
                        assertThat(timeoutUpdated.get().getValue().getDeadline())
                            .isGreaterThan(job.getDeadline());
                      });
            });
  }

  @Test
  void shouldReadProcessInstanceRecords() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    // when
    final ProcessInstanceEvent processInstance =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("simpleProcess")
            .latestVersion()
            .variables(Map.of("test", 1))
            .send()
            .join();

    // then
    Awaitility.await()
        .untilAsserted(
            () -> {
              final List<Record<ProcessInstanceRecordValue>> processRecords =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .processInstanceRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getRecordType() == RecordType.EVENT)
                      .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
                      .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.UNSPECIFIED)
                      .limit(4)
                      .collect(Collectors.toList());

              assertThat(processRecords)
                  .hasSize(4)
                  .extracting(Record::getIntent)
                  .contains(
                      ProcessInstanceIntent.ELEMENT_ACTIVATING,
                      ProcessInstanceIntent.ELEMENT_ACTIVATED,
                      ProcessInstanceIntent.ELEMENT_COMPLETING,
                      ProcessInstanceIntent.ELEMENT_COMPLETED);

              final ProcessInstanceRecordValue processInstanceRecord =
                  processRecords.get(0).getValue();
              assertThat(processInstanceRecord.getProcessDefinitionKey())
                  .isEqualTo(processInstance.getProcessDefinitionKey());
              assertThat(processInstanceRecord.getBpmnProcessId())
                  .isEqualTo(processInstance.getBpmnProcessId());
              assertThat(processInstanceRecord.getVersion())
                  .isEqualTo(processInstance.getVersion());
              assertThat(processInstanceRecord.getBpmnElementType())
                  .isEqualTo(BpmnElementType.PROCESS);
              assertThat(processInstanceRecord.getBpmnEventType())
                  .isEqualTo(BpmnEventType.UNSPECIFIED);
            });
  }

  @Test
  void shouldPrintRecords() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess").startEvent().endEvent().done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    // when
    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(Map.of("test", 1))
        .send()
        .join();

    // then
    final RecordStream recordStream = RecordStream.of(zeebeEngine.getRecordStreamSource());
    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<ProcessInstanceRecordValue>> processRecords =
                  StreamSupport.stream(recordStream.processInstanceRecords().spliterator(), false)
                      .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
                      .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.UNSPECIFIED)
                      .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED)
                      .findFirst();

              assertThat(processRecords).isNotEmpty();
            });

    recordStream.print(true);
  }

  @Test
  void shouldIncreaseTheTime() {
    // given
    zeebeClient
        .newDeployCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .intermediateCatchEvent()
                .timerWithDuration("P1D")
                .endEvent()
                .done(),
            "process.bpmn")
        .send()
        .join();

    zeebeClient.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<TimerRecordValue>> timerCreated =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .timerRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getIntent() == TimerIntent.CREATED)
                      .findFirst();

              assertThat(timerCreated).isNotEmpty();
            });

    // when
    zeebeEngine.increaseTime(Duration.ofDays(1));

    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<ProcessInstanceRecordValue>> processCompleted =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .processInstanceRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
                      .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.UNSPECIFIED)
                      .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED)
                      .findFirst();

              assertThat(processCompleted).isNotEmpty();
            });
  }

  @Test
  void shouldDeployForm() {
    // given

    // when
    final DeploymentEvent deployment =
        zeebeClient
            .newDeployResourceCommand()
            .addResourceFromClasspath("form/test-form-1.form")
            .send()
            .join();

    // then
    assertThat(deployment.getKey()).isPositive();
    assertThat(deployment.getForm()).isNotEmpty();

    final Form form = deployment.getForm().get(0);

    assertThat(form.getVersion()).isEqualTo(1);
    assertThat(form.getResourceName()).isEqualTo("form/test-form-1.form");
    assertThat(form.getFormKey()).isPositive();
    assertThat(form.getFormId()).isEqualTo("Form_0w7r08e");
  }

  @Test
  void shouldBroadcastSignal() {
    // given
    zeebeClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .signal("signal")
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    // when
    final BroadcastSignalResponse response =
        zeebeClient.newBroadcastSignalCommand().signalName("signal").send().join();

    // then
    assertThat(response.getKey()).isPositive();
  }

  @Test
  public void shouldNotSendResponseWhenNoCorrespondingRequest() {
    // given
    zeebeClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("process")
                .startEvent()
                .intermediateThrowEvent("signal", b -> b.signal("signalName"))
                .endEvent()
                .done(),
            "process.bpmn")
        .send()
        .join();

    // when
    zeebeClient.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();

    // then
    Awaitility.await()
        .untilAsserted(
            () -> {
              final Optional<Record<ProcessInstanceRecordValue>> processCompleted =
                  StreamSupport.stream(
                          RecordStream.of(zeebeEngine.getRecordStreamSource())
                              .processInstanceRecords()
                              .spliterator(),
                          false)
                      .filter(r -> r.getValue().getBpmnElementType() == BpmnElementType.PROCESS)
                      .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.UNSPECIFIED)
                      .filter(r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED)
                      .findFirst();

              assertThat(processCompleted).isNotEmpty();
            });

    assertThat(responseIntents).doesNotContain(SignalIntent.BROADCASTED);
  }

  @Test
  void shouldBroadcastSignalWithVariables() {
    // given
    zeebeClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .signal("signal")
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();

    // when
    final BroadcastSignalResponse response =
        zeebeClient
            .newBroadcastSignalCommand()
            .signalName("signal")
            .variable("foo", "bar")
            .send()
            .join();

    // then
    assertThat(response.getKey()).isPositive();
  }

  @Test
  void shouldStreamJobs() {
    // given
    final var jobs = new CopyOnWriteArrayList<ActivatedJob>();
    final var deployment = deploySingleTaskProcess();

    // when
    final var stream =
        zeebeClient
            .newStreamJobsCommand()
            .jobType("jobType")
            .consumer(jobs::add)
            .fetchVariables(List.of("test"))
            .workerName("worker")
            .timeout(Duration.ofMinutes(1))
            .send();

    // then - since streams cannot receive jobs created before they are registered, and registration
    // is asynchronous, we just create some jobs until we receive at least one
    try {
      Awaitility.await("until we've received some jobs")
          .untilAsserted(
              () -> {
                createSingleTaskInstance(Map.of("test", 1));
                assertThat(jobs).isNotEmpty();
              });

      assertThat(jobs)
          .allSatisfy(
              job -> {
                assertThat(job.getBpmnProcessId()).isEqualTo("simpleProcess");
                assertThat(job.getProcessDefinitionKey())
                    .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
                assertThat(job.getRetries()).isEqualTo(3);
                assertThat(job.getType()).isEqualTo("jobType");
                assertThat(job.getWorker()).isEqualTo("worker");
              });
    } finally {
      stream.cancel(true);
    }
  }

  @Test
  void shouldYieldJobIfBlocked() {
    // given
    final var deployment = deploySingleTaskProcess();
    final var latch = new CountDownLatch(1);
    final var recordStream = RecordStream.of(zeebeEngine.getRecordStreamSource());
    // we create a large variable to trigger back pressure on the client side, otherwise it would
    // take tens of thousands of them to reach the hardcoded 32KB threshold
    final var variables = Map.<String, Object>of("foo", "x".repeat(1024 * 1024));

    // when
    final var stream =
        zeebeClient
            .newStreamJobsCommand()
            .jobType("jobType")
            .consumer(job -> Uninterruptibles.awaitUninterruptibly(latch))
            .fetchVariables(List.of("foo"))
            .workerName("worker")
            .timeout(Duration.ofMinutes(1))
            .send();

    // then - since streams cannot receive jobs created before they are registered, and registration
    // is asynchronous, we just create some jobs until at least one job is yielded
    final Map<Long, Record<JobRecordValue>> yieldedJobs = new HashMap<>();
    try {
      Awaitility.await("until some jobs are yielded")
          .pollInSameThread()
          .pollInterval(Duration.ofMillis(50))
          .untilAsserted(
              () -> {
                createSingleTaskInstance(variables);
                new JobRecordStreamFilter(recordStream.jobRecords())
                    .withIntent(JobIntent.YIELDED).stream()
                        .forEach(job -> yieldedJobs.put(job.getKey(), job));
                assertThat(yieldedJobs).isNotEmpty();
              });
      latch.countDown();

      // since we're not exactly tracking which jobs are yielded, we can only do a coarse validation
      // that the right job was yielded
      assertThat(yieldedJobs)
          .allSatisfy(
              (key, job) -> {
                assertThat(job.getIntent()).isEqualTo(JobIntent.YIELDED);
                assertThat(job.getValue().getBpmnProcessId()).isEqualTo("simpleProcess");
                assertThat(job.getValue().getProcessDefinitionKey())
                    .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
                assertThat(job.getValue().getRetries()).isEqualTo(3);
                assertThat(job.getValue().getType()).isEqualTo("jobType");
              });
    } finally {
      stream.cancel(true);
    }
  }

  private ProcessInstanceEvent createSingleTaskInstance(final Map<String, Object> variables) {
    return zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(variables)
        .send()
        .join();
  }

  private DeploymentEvent deploySingleTaskProcess() {
    return zeebeClient
        .newDeployResourceCommand()
        .addProcessModel(
            Bpmn.createExecutableProcess("simpleProcess")
                .startEvent()
                .serviceTask("task", (task) -> task.zeebeJobType("jobType"))
                .endEvent()
                .done(),
            "simpleProcess.bpmn")
        .send()
        .join();
  }
}
