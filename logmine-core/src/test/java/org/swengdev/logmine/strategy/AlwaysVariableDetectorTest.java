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

package org.swengdev.logmine.strategy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for AlwaysVariableDetector. */
public class AlwaysVariableDetectorTest {

  private final VariableDetector detector = new AlwaysVariableDetector();

  @Test
  public void testAlwaysReturnsTrue() {
    assertTrue(detector.isVariable("anything"));
    assertTrue(detector.isVariable("123"));
    assertTrue(detector.isVariable("INFO"));
    assertTrue(detector.isVariable("@#$%"));
    assertTrue(detector.isVariable(""));
  }

  @Test
  public void testNullValue() {
    assertTrue(detector.isVariable(null));
  }
}
