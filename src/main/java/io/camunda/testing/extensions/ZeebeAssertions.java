package io.camunda.testing.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.camunda.community.eze.EmbeddedZeebeEngine;
import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@EmbeddedZeebeEngine
@ExtendWith(ZeebeAssertionsExtension.class)
public @interface ZeebeAssertions {}
