/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */

package io.camunda.zeebe.process.test.engine.db;

import io.camunda.zeebe.db.impl.ZeebeDbConstants;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.agrona.ExpandableArrayBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BytesTest {
  @Test
  public void compareEqual() {
    // given
    final OffsetDateTime date =
        OffsetDateTime.of(2023, 10, 5, 15, 50, 0, 0, ZoneOffset.of("+02:00"));

    final OffsetDateTime sameDate =
        OffsetDateTime.of(2023, 10, 5, 15, 50, 0, 0, ZoneOffset.of("+02:00"));

    final ExpandableArrayBuffer dateKeyBuffer = new ExpandableArrayBuffer();
    dateKeyBuffer.putLong(0, date.toInstant().toEpochMilli(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);

    final ExpandableArrayBuffer sameDateKeyBuffer = new ExpandableArrayBuffer();
    sameDateKeyBuffer.putLong(
        0, sameDate.toInstant().toEpochMilli(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);

    // when
    final int result = date.compareTo(sameDate);

    // then
    Assertions.assertEquals(0, result);
  }

  @Test
  public void compareLaterDateWithEarlierDate() {
    // given
    // 2023.10.10 15:50:00 This date will look as follows when converted to bytes array:
    // [0, 0, 1, -117, 25...]
    final OffsetDateTime earlierDate =
        OffsetDateTime.of(2023, 10, 10, 15, 50, 0, 0, ZoneOffset.of("+02:00"));

    // 2023.11.5 15:50:00 This date will look as follows when converted to bytes array:
    // [0, 0, 1, -117, -97...]
    final OffsetDateTime laterDate =
        OffsetDateTime.of(2023, 11, 5, 15, 50, 0, 0, ZoneOffset.of("+02:00"));

    final ExpandableArrayBuffer earlierDateKeyBuffer = new ExpandableArrayBuffer();
    earlierDateKeyBuffer.putLong(
        0, earlierDate.toInstant().toEpochMilli(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);

    final ExpandableArrayBuffer laterDateKeyBuffer = new ExpandableArrayBuffer();
    laterDateKeyBuffer.putLong(
        0, laterDate.toInstant().toEpochMilli(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);

    final Bytes erlierDateBytes = Bytes.fromExpandableArrayBuffer(earlierDateKeyBuffer);
    final Bytes laterDateBytes = Bytes.fromExpandableArrayBuffer(laterDateKeyBuffer);

    System.out.println(Arrays.toString(erlierDateBytes.toBytes()));
    System.out.println(Arrays.toString(laterDateBytes.toBytes()));

    // when
    final int result = laterDateBytes.compareTo(erlierDateBytes);

    // then
    // The result should be positive as 2023.11.5 15:50:00 comes after 2023.10.10 15:50:00
    // The comparison should return 1 despite the fact that later date contains -97
    // when represented as bytes array
    Assertions.assertEquals(1, result);
  }

  @Test
  public void compareEarlierDateWithLaterDate() {
    // given
    // 2023.10.10 15:50:00 This date will look as follows when converted to bytes array:
    // [0, 0, 1, -117, 25...]
    final OffsetDateTime earlierDate =
        OffsetDateTime.of(2023, 10, 10, 15, 50, 0, 0, ZoneOffset.of("+02:00"));

    // 2023.11.5 15:50:00 This date will look as follows when converted to bytes array:
    // [0, 0, 1, -117, -97...]
    final OffsetDateTime laterDate =
        OffsetDateTime.of(2023, 11, 5, 15, 50, 0, 0, ZoneOffset.of("+02:00"));

    final ExpandableArrayBuffer earlierDateKeyBuffer = new ExpandableArrayBuffer();
    earlierDateKeyBuffer.putLong(
        0, earlierDate.toInstant().toEpochMilli(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);

    final ExpandableArrayBuffer laterDateKeyBuffer = new ExpandableArrayBuffer();
    laterDateKeyBuffer.putLong(
        0, laterDate.toInstant().toEpochMilli(), ZeebeDbConstants.ZB_DB_BYTE_ORDER);

    final Bytes erlierDateBytes = Bytes.fromExpandableArrayBuffer(earlierDateKeyBuffer);
    final Bytes laterDateBytes = Bytes.fromExpandableArrayBuffer(laterDateKeyBuffer);

    // when
    final int result = erlierDateBytes.compareTo(laterDateBytes);

    // then
    // The result should be negative as 2023.10.10 15:50:00 comes before 2023.11.5 15:50:00
    // The comparison should return 1 despite the fact that later date contains -97
    // when represented as bytes array
    Assertions.assertEquals(-1, result);
  }
}
