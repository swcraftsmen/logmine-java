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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/** Tests for NeverVariableDetector. */
public class NeverVariableDetectorTest {

  private final VariableDetector detector = new NeverVariableDetector();

  @Test
  public void testAlwaysReturnsFalse() {
    assertFalse(detector.isVariable("anything"));
    assertFalse(detector.isVariable("123"));
    assertFalse(detector.isVariable("INFO"));
    assertFalse(detector.isVariable("@#$%"));
    assertFalse(detector.isVariable(""));
    assertFalse(detector.isVariable(null));
  }
}
