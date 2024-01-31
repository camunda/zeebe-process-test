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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.ClientException;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.BroadcastSignalResponse;
import io.camunda.zeebe.client.api.response.BrokerInfo;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.EvaluateDecisionResponse;
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
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.TimerIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.BpmnEventType;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.util.VersionUtil;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  @BeforeEach
  void setupGrpcServer() {
    zeebeEngine = EngineFactory.create();
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
                  .newThrowErrorCommand(job.getKey())
                  .errorCode("0xCAFE")
                  .errorMessage("What just happened.")
                  .send()
                  .join();

              Awaitility.await()
                  .untilAsserted(
                      () -> {
                        final Optional<Record<ProcessInstanceRecordValue>> boundaryEvent =
                            StreamSupport.stream(
                                    RecordStream.of(zeebeEngine.getRecordStreamSource())
                                        .processInstanceRecords()
                                        .spliterator(),
                                    false)
                                .filter(
                                    r -> r.getIntent() == ProcessInstanceIntent.ELEMENT_COMPLETED)
                                .filter(
                                    r ->
                                        r.getValue().getBpmnElementType()
                                            == BpmnElementType.BOUNDARY_EVENT)
                                .filter(r -> r.getValue().getBpmnEventType() == BpmnEventType.ERROR)
                                .findFirst();

                        assertThat(boundaryEvent).isNotEmpty();
                      });
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
            .variables(Map.of("foo", "bar"))
            .send()
            .join();

    // then
    assertThat(response.getKey()).isPositive();
  }
}
