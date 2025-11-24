/*
 * Copyright 2024 Zachary Huang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swengdev.logmine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for ProcessingMode enum. */
public class ProcessingModeTest {

  @Test
  public void testStreamingMode() {
    ProcessingMode mode = ProcessingMode.STREAMING;

    assertFalse(mode.storesRawLogs());
    assertNotNull(mode.description());
    assertTrue(mode.description().contains("Streaming"));
  }

  @Test
  public void testBatchMode() {
    ProcessingMode mode = ProcessingMode.BATCH;

    assertTrue(mode.storesRawLogs());
    assertNotNull(mode.description());
    assertTrue(mode.description().contains("Batch"));
  }

  @Test
  public void testMatchPattern() {
    ProcessingMode mode = ProcessingMode.STREAMING;

    String result = mode.match(() -> "streaming", () -> "batch");

    assertEquals("streaming", result);

    mode = ProcessingMode.BATCH;
    result = mode.match(() -> "streaming", () -> "batch");

    assertEquals("batch", result);
  }

  @Test
  public void testEnumValues() {
    ProcessingMode[] modes = ProcessingMode.values();

    assertEquals(2, modes.length);
    assertEquals(ProcessingMode.STREAMING, modes[0]);
    assertEquals(ProcessingMode.BATCH, modes[1]);
  }

  @Test
  public void testValueOf() {
    assertEquals(ProcessingMode.STREAMING, ProcessingMode.valueOf("STREAMING"));
    assertEquals(ProcessingMode.BATCH, ProcessingMode.valueOf("BATCH"));
  }

  @Test
  public void testDescriptionContent() {
    assertTrue(ProcessingMode.STREAMING.description().contains("real-time"));
    assertTrue(ProcessingMode.BATCH.description().contains("batch"));
  }
}
