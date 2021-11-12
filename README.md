# Camunda Cloud Testing project

This project is in very early stages of development.

## Hints

The following details are due to the project being in early stages of development:

* Works only in Java 11 or higher (in the future we aim to support Java 8 as well)
* Works only in JUnit 5 (we might stick to that, but are happy to receive feedback)
* Zeebe is an asynchronous engine, so use `Thread.sleep(100)` to wait for Zeebe to process the last request (in the
  future we want to offer a method to wait for Zeebe to become idle)
* You need to inject the following fields in your tests to make the extension work:

```java

@ZeebeAssertions
class DeploymentAssertTest {

  private ZeebeClient client;
  private ZeebeEngine engine;
  private RecordStreamSource recordStreamSource;
```

## Usage

Best place right now is to look at the tests in `io.camunda.testing.assertions`
