[![Maven Central](https://img.shields.io/maven-central/v/io.camunda/zeebe-process-test)](https://search.maven.org/search?q=g:io.camunda%20a:zeebe-process-test)
# Zeebe Process Test

**This project is in very early stages of development.**

This project allows you to unit test your Camunda Cloud BPMN processes. It will spin up an in-memory
engine and provide you with a set of assertions you can use to verify your process behaves as expected.

## Prerequisites
* Java 11 or higher (in the future Java 8 will be supported as well)
* JUnit 5

## Getting Started

### Dependency
Add the following dependency to your project
```xml
<dependency>
  <groupId>io.camunda</groupId>
  <artifactId>zeebe-process-test-extension</artifactId>
  <version>X.Y.Z</version>
  <scope>test</scope>
</dependency>
```

**Note**: This snapshot version is bound to change in the future. Only use this when you want to play around with the project.

### Annotation
Annotate your test class with the `@ZeebeProcessTest` annotation. This annotation will do a couple of things:

1. It will create and start the in memory engine. This will be a new engine for each test case.
2. It will create a client which can be used to interact with the engine.
3. It will (optionally) inject 3 fields in your test class:
   1. `InMemoryEngine` - This is the engine that will run your process. It will provide some basic functionality
       to help you write your tests, such as waiting for an idle state and increasing the time.
   2. `ZeebeClient` - This is the client that allows you to communicate with the engine.
       It allows you to send commands to the engine.
   3. `RecordStreamSource` - This gives you access to all the records that are processed by the engine.
      It is what the assertions use to verify expectations. This grants you the freedom to create your own assertions.
4. It will take care of cleaning up the engine and client when the testcase is finished.

Example:
```java
@ZeebeProcessTest
class DeploymentAssertTest {
  private InMemoryEngine engine;
  private ZeebeClient client;
  private RecordStreamSource recordStreamSource;
}
```

### Assertions

There are multiple entrypoints for starting assertions:

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

```java
ProcessInstanceResult event = client.newCreateInstanceCommand()
  .bpmnProcessId("<processId>")
  .latestVersion()
  .withResult()
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

### Waiting for idle state

> **Warning!** Waiting for idle state is a new feature. When the engine is detected to be idle it
> will wait 10ms before checking again. If it is still idle at that stage it is considered to be in
> an idle state.
>
> **We do not know if the 10ms delay is sufficient. Using it could result in flaky tests!**
>
> Any feedback about the wait for idle state is highly appreciated! Please let us know if the delay should be higher, or configurable.

The engine allows you to wait until it is idle before continuing with your test.
The engine provides 2 methods for this:

1. `engine.waitForIdleState()` - This method will cause your test to stop executing until the engine has reached the idle state.
2. `engine.runOnIdleState(Runnable)` - This method will run your runnable once it has reached an idle state. Your test will continue executing without waiting.

We have defined an idle state as a state in which the process engine makes no progress and is waiting for new commands or events to trigger.
Once the engine has detected it has become idle it will wait for a delay (10ms) and check if it is still idle.
If this is the case it is considered to be in idle state and continue your test / execute the runnables.

## Examples
For example tests the best place to look right now is the tests in the QA module.

## Project Structure
The project consists of 5 different modules:
1. Api
    - This module contains public interfaces. It should always be Java 8 compatible.
2. Assertions
    - This module contains all the assertions. It should always be Java 8 compatible.
3. Engine
    - This module contains the in memory engine. It is not bound to a specific Java version.
      Therefore, it is not recommended to depend on this module.
4. Extension
    - This module contains the annotation for your unit tests. As of now it depends on the engine
      and thus is not bound to a specific Java version. In the future this should be Java 8 compatible.
5. QA
   - This module contains our QA tests. There is no reason to depend on this module. It is not bound to a specific Java version.

## Contributing
Please refer to the [Contributions Guide](/CONTRIBUTING.md).

## Credits

Special thanks to the creators of the [Embedded Zeebe Engine](https://github.com/camunda-community-hub/eze)
and [Camunda BPMN Assert](https://github.com/camunda/camunda-bpm-assert).
This project was heavily inspired by these solutions.
