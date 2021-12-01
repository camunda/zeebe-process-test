# Camunda Cloud Testing project

This project is in very early stages of development.

## Getting Started

Add the following dependency to your project
```
<dependency>
  <groupId>io.camunda</groupId>
  <artifactId>zeebe-bpmn-assert</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

**Note**: This snapshot version is bound to change in the future. Only use this when you want to play around with the project.

## Hints

The following details are due to the project being in early stages of development:

* Works only in Java 11 or higher (in the future we aim to support Java 8 as well)
* Works only in JUnit 5 (we might stick to that, but are happy to receive feedback)
* Zeebe is an asynchronous engine, so use `Thread.sleep(100)` to wait for Zeebe to process the last request (in the
  future we want to offer a method to wait for Zeebe to become idle)
* The extension can inject the following fields into your test:

```java
@ZeebeProcessTest
class DeploymentAssertTest {

  private ZeebeClient client;
  private ZeebeEngine engine;
  private RecordStreamSource recordStreamSource;
```

## Usage

For example tests the best place to look right now is the tests in `io.camunda.testing.assertions`

There are multiple entrypoints for starting assertions.

#### Deployment Assertions
```java
DeploymentEvent event = client.newDeployCommand()
  .addResourceFromClasspath("my-process.bpmn")
  .send()
  .join();
DeploymentAssert assertions = BpmnAssert.assertThat(event);
```

#### Process Instance Assertions
Started by manually sending an event:
```java
ProcessInstanceEvent event = client.newCreateInstanceCommand()
  .bpmnProcessId("<processId>")
  .latestVersion()
  .send()
  .join();
ProcessInstanceAssert assertions = BpmnAssert.assertThat(event);
```

Started by a timer:
```java
Optional<InspectedProcessInstance> firstProcessInstance = InspectionUtility.findProcessEvents()
  .triggeredByTimer(ProcessPackTimerStartEvent.TIMER_ID)
  .findFirstProcessInstance();
ProcessInstanceAssert assertions = BpmnAssert.assertThat(firstProcessInstance.get());
```

Started by a call activity:
```java
Optional<InspectedProcessInstance> firstProcessInstance = InspectionUtility.findProcessInstances()
  .withParentProcessInstanceKey(<key>)
  .withBpmnProcessId("<called process id>")
  .findFirstProcessInstance();
ProcessInstanceAssert assertions = BpmnAssert.assertThat(firstProcessInstance.get());
```

#### Job Assertions
```java
ActivateJobsResponse response = client.newActivateJobsCommand()
  .jobType("<jobType>")
  .maxJobsToActivate(1)
  .send()
  .join();
ActivatedJob activatedJob = response.getJobs().get(0);
JobAssert assertions = BpmnAssert.assertThat(activatedJob);
```

#### Message Assertions
```java
PublishMessageResponse response = client
  .newPublishMessageCommand()
  .messageName("<messageName>")
  .correlationKey("<correlationKey>")
  .send()
  .join();
MessageAssert assertions = BpmnAssert.assertThat(response);
```
