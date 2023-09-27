[![Maven Central](https://img.shields.io/maven-central/v/io.camunda/zeebe-process-test-root)](https://search.maven.org/search?q=g:io.camunda%20zeebe-process-test)

# Zeebe Process Test

This project allows you to unit test your Camunda Platform 8 BPMN processes. It will start a Zeebe test engine
and provide you with a set of assertions you can use to verify your process behaves as expected.

## Prerequisites

* Java 17+ when running with an embedded engine (`zeebe-process-test-extension`)
* Java 8+ and Docker when running using testcontainers (`zeebe-process-test-extension-testcontainer`)
* JUnit 5

**NOTE**: If you use **Spring** and want to write tests, please use `spring-zeebe-test` as a wrapper around Zeebe Process Test to hook everything into the Spring lifecycle. See [Spring Zeebe: Writing test cases](https://github.com/camunda-community-hub/spring-zeebe#writing-test-cases) for details.

## Getting Started

### Dependency

Zeepe Process Test provides you with two dependencies. Which one you need to use is dependent on the
Java version you are using.

#### Embedded (JDK 17+)

If you are building your project with JDK 17+ you can make use of an embedded Zeebe test engine. The
advantage of using this instead of the testcontainer version is that this is the faster solution.
This also does not require Docker to be running. There is also a downside to this solution. The JDK
requirement is bound to the Java version of the Zeebe engine. Whenever this Java version changes,
you'd either have to [switch to the testcontainer version](#switching-between-testcontainers-and-embedded),
or upgrade your own JDK to match Zeebe engine.

```xml
<dependency>
  <groupId>io.camunda</groupId>
  <artifactId>zeebe-process-test-extension</artifactId>
  <version>X.Y.Z</version>
  <scope>test</scope>
</dependency>
```

#### Testcontainers (JDK 8+)

If you are building your project with a JDK that's lower than 17 you need to use this dependency. It
starts a testcontainer in which a Zeebe test engine is running. The advantage of using this version
instead of the embedded version is that your code can be implemented independently of the Java
version that is used by the Zeebe engine. This has some downsides: Testcontainers provide some
overhead, which means tests will be slower. There is also the extra requirement that Docker must be
running to execute the tests.

```xml
<dependency>
  <groupId>io.camunda</groupId>
  <artifactId>zeebe-process-test-extension-testcontainer</artifactId>
  <version>X.Y.Z</version>
  <scope>test</scope>
</dependency>
```

Using Maven profiles you can also [switch the test dependencies based on the available Java version](https://github.com/camunda-community-hub/camunda-8-process-solution-template/commit/128be0ead988404c5c746ae96b47fe1138bf2a83).

### Spring support

If you use the Spring framework or Spring Boot and you want to write tests, please use `spring-zeebe-test` as a wrapper around this library. This will hook everything into the Spring lifecycle. See [Spring Zeebe: Writing test cases](https://github.com/camunda-community-hub/spring-zeebe#writing-test-cases) for details.

### Annotation

Annotate your test class with the `@ZeebeProcessTest` annotation. This annotation will do a couple of things:

1. It will manage the [lifecycle](#engine-lifecycle) of the testcontainer / embedded test engine
2. It will create a client which can be used to interact with the engine.
   1. A [custom ObjectMapper](#custom-mapper) can be provided to the client by simply create and configure an
      instance of it in the test class.
3. It will (optionally) inject 3 fields in your test class:
   1. `ZeebeTestEngine` - This is the engine that will run your process. It will provide some basic functionality
      to help you write your tests, such as waiting for an idle state and increasing the time.
   2. `ZeebeClient` - This is the client that allows you to  send commands to the engine, such as
      starting a process instance. The interface of this client is identical to the interface you
      use to connect to a real Zeebe engine.
   3. `RecordStream` - This gives you access to all the records that are processed by the engine.
      Assertions use the records for verifying expectations. This grants you the freedom to create your own assertions.

Example:

```java
// When using the embedded test engine (Java 17+)
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;

// When using testcontainers (Java 8+)
import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;

@ZeebeProcessTest
class DeploymentAssertTest {
  private ZeebeTestEngine engine;
  private ZeebeClient client;
  private RecordStream recordStream;
}
```

### Switching between testcontainers and embedded

Switching between testcontainers and embedded is very easy to do. You'll have to take two steps:

1. Switch to the relevant dependency

- Testcontainers: `zeebe-process-test-extension-testcontainer`
- Embedded: `zeebe-process-test-extension`

2. Change the import of `@ZeebeProcessTest`

- Testcontainers: `import io.camunda.zeebe.process.test.extension.testcontainer.ZeebeProcessTest;`
- Embedded: `import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;`

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

#### Incident Assertions

Via a process instance

```java
ProcessInstanceEvent event = client.newCreateInstanceCommand()
  .bpmnProcessId("<processId>")
  .latestVersion()
  .send()
  .join();
IncidentAssert assertions = BpmnAssert.assertThat(event)
  .extractingLatestIncident();
```

Via a job:

```java
ActivateJobsResponse response = client.newActivateJobsCommand()
  .jobType("<jobType>")
  .maxJobsToActivate(1)
  .send()
  .join();
ActivatedJob activatedJob = response.getJobs().get(0);
IncidentAssert assertions = BpmnAssert.assertThat(activatedJob)
  .extractingLatestIncident();
```

#### Form Assertions

```java
DeploymentEvent event = client.newDeployCommand()
  .addResourceFromClasspath("my-form.form")
  .send()
  .join();
FormAssert assertions =  BpmnAssert.assertThat(event).extractingFormByFormId("<form id>");
```

### Custom Mapper

A custom `ObjectMapper` can be provided to the `Zeebe Client`

```java
@ZeebeProcessTest
class MyProcessTest {
  private ZeebeTestEngine engine;
  private ZeebeClient client;
  private ObjectMapper mapper = setupMapper();


  private ObjectMapper setupMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    // custom configuration
    return objectMapper;
  }
}
```

### Waiting for idle and busy state

#### Wait for idle state

> **Warning!** Waiting for idle state is a new feature. When the engine is detected to be idle it
> will wait 30ms before checking again. If it is still idle at that stage it is considered to be in
> an idle state.
>
> **We do not know if the 30ms delay is sufficient. Using it could result in flaky tests!**
>
> Any feedback about the wait for idle state is highly appreciated! Please let us know if the delay should be higher, or configurable.

`engine.waitForIdleState(timeout)` will cause your test to stop executing until the engine has
reached an idle state. If the engine does not reach an idle state within the specified timeout a
`TimeoutException` will be thrown.

We have defined an idle state as a state in which the engine makes no progress and is waiting for
new commands or events to trigger. Once the engine has detected it has become idle it will wait for
a delay (30ms) and check if it is still idle. If this is the case it is considered to be in idle
state and continue your test.

#### Wait for busy state

`engine.waitForBusyState(timeout)` will cause your test to stop executing until the engine has
reached a busy state. If the engine does not reach a busy state within the specified timeout a
`TimeoutException` will be thrown.

We consider the engine to have reached a busy state when any new record / command is processed since
we've started waiting.

Waiting for a busy state is useful in scenarios where you're expecting the engine to start doing
something, without explicitly triggering it yourself. An example of this would be a process with a
timer event. We can increase the time of the engine, but we cannot trigger the timer explicitly.
Because of this we should wait for a busy state after increasing the engine time.

### Inspections

There are entrypoints for starting inspections on process engine data

#### Deployment Inspections

Find BPMN element id by its name (also works for processes):

```java
import static io.camunda.zeebe.process.test.inspections.ProcessDefinitionInspectionUtility.getBpmnElementId;
String elementId = getBpmnElementId("<element name>");
```

Find BPMN element id in defined BPMN process by its name:

```java
import static io.camunda.zeebe.process.test.inspections.ProcessDefinitionInspectionUtility.getBpmnElementId;
String elementId = getBpmnElementId("<bpmn process id>","<element name>");
```

#### Form Inspections

Find latest version of a form:

```java
import static io.camunda.zeebe.process.test.inspections.FormInspectionsUtility.findLatestFormById;
Optional<FormMetadataValue> latestForm = findLatestFormById("<form id>");
```

## Examples

[Examples](examples/README.md)

## Engine lifecycle

The lifecycle of the engine will be fully managed by the extension. The lifecycle for both
extensions differs slightly.

**Testcontainers**
1. Before the test suite start the testcontainer
2. Before each test stop the current Zeebe test engine (if applicable) and create a new Zeebe test
engine for the next test
3. Run the test
4. After the test suite stop the testcontainer

**Embedded**
1. Before each test create a new Zeebe test engine
2. Run the test
3. After each test stop the Zeebe test engine

## Project Structure

The project consists of 5 different modules:
1. Api ([api](/api))
- This module contains public interfaces. It should always be Java 8 compatible.
2. Assertions ([assertions](/assertions))
- This module contains all the assertions. It should always be Java 8 compatible.
3. Engine ([engine](/engine))
- This module contains the in memory engine. It is not bound to a specific Java version.
Therefore, it is not recommended to depend on this module.
4. Engine agent ([engine-agent](engine-agent))
- This module is a wrapper around the engine. It enables running the engine in a docker container.
5. Engine protocol ([engine-protocol](/engine-protocol))
- This module defines the gRPC protocol used for communicating with the engine agent.
6. Extension ([extension](/extension))
- This module contains the extension for using the embedded test engine.
7. Extension testcontainer ([extension-testcontainer](/extension-testcontainer))
- This module contains the extension for using the test engine in a testcontainer.
8. Filters ([filters](/filters))
- This module contains filters that can be used to filter a list of Zeebe records.
9. QA ([qa](/qa))
- This module contains our QA tests. There is no reason to depend on this module. It is not bound to a specific Java version.

## Backwards compatibility

Starting from release 1.4.0 we will ensure backwards compatibility in this project. This will be
limited to the extension, the assertions and the public interfaces.
We will aim to be backwards compatible on other modules, however this is not guaranteed.
Using / extending these are at your own risk.

## Contributing

Please refer to the [Contributions Guide](/CONTRIBUTING.md).

## Credits

Special thanks to the creators of the [Embedded Zeebe Engine](https://github.com/camunda-community-hub/eze)
and [Camunda BPMN Assert](https://github.com/camunda/camunda-bpm-assert).
This project was heavily inspired by these solutions.

## License

Zeebe Process Test source files are made available under the [Apache License, Version 2.0](/licenses/APACHE-2.0.txt)
except for the parts listed below, which are made available under the [Zeebe Community License, Version 1.1](/licenses/ZEEBE-COMMUNITY-LICENSE-1.1.txt).

Available under the [Zeebe Community License, Version 1.1](/licenses/ZEEBE-COMMUNITY-LICENSE-1.1.txt):
- Engine ([engine](/engine))
- Engine Agent ([engine-agent](/engine-agent))
- Extension ([extension](/extension))
