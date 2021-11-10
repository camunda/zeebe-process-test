package io.camunda.testing.assertions;

import org.assertj.core.api.AbstractAssert;
import org.camunda.community.eze.RecordStreamSource;

/** Assertions for incidents. An incident is identified by its incident key */
public class IncidentAssert extends AbstractAssert<IncidentAssert, Long> {

  private final RecordStreamSource recordStreamSource;

  public IncidentAssert(final long incidentKey, final RecordStreamSource recordStreamSource) {
    super(incidentKey, IncidentAssert.class);
    this.recordStreamSource = recordStreamSource;
  }
}
