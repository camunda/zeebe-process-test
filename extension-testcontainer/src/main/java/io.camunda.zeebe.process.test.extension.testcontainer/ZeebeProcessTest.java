package io.camunda.zeebe.process.test.extension.testcontainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation can be used to test BPMN processes. It will make use of testcontainers to run an
 * in memory engine. To use this annotation Java 8 or higher is required. Docker also needs to be
 * running.
 *
 * Annotating test classes with this annotation will do a couple of things:
 *
 * <ul>
 *   <li>It start a docker container running and in memory engine.
 *   <li>It will create a client which can be used to interact with the engine.
 *   <li>It will (optionally) inject 3 fields in your test class:
 *       <ul>
 *         <li>InMemoryEngine - This is the engine that will run your process. It will provide some
 *             basic functionality to help you write your tests, such as waiting for an idle state
 *             and increasing the time.
 *         <li>ZeebeClient - This is the client that allows you to communicate with the engine. It
 *             allows you to send commands to the engine.
 *         <li>RecordStream - This gives you access to all the records that are processed by
 *             the engine. It is what the assertions use to verify expectations. This grants you the
 *             freedom to create your own assertions.
 *       </ul>
 *   <li>It will take care of cleaning up the engine and client when the testcase is finished.
 * </ul>
 *
 * @since Java 8
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(ZeebeProcessTestExtension.class)
public @interface ZeebeProcessTest {

}
