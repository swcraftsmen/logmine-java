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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Tests for CustomVariableDetector. */
public class CustomVariableDetectorTest {

  @Test
  public void testWithPatterns() {
    Pattern numberPattern = Pattern.compile("\\d+");
    Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    Set<Pattern> patterns = new HashSet<>();
    patterns.add(numberPattern);
    patterns.add(emailPattern);

    VariableDetector detector = new CustomVariableDetector(patterns, new HashSet<>(), false);

    assertTrue(detector.isVariable("123"));
    assertTrue(detector.isVariable("user@example.com"));
    assertFalse(detector.isVariable("NotANumber"));
    assertFalse(detector.isVariable("not-an-email"));
  }

  @Test
  public void testWithSinglePattern() {
    Pattern uuidPattern =
        Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    Set<Pattern> patterns = new HashSet<>();
    patterns.add(uuidPattern);

    VariableDetector detector = new CustomVariableDetector(patterns, new HashSet<>(), false);

    assertTrue(detector.isVariable("550e8400-e29b-41d4-a716-446655440000"));
    assertFalse(detector.isVariable("not-a-uuid"));
  }

  @Test
  public void testEmptyPatternList() {
    VariableDetector detector = new CustomVariableDetector(new HashSet<>(), new HashSet<>(), false);

    assertFalse(detector.isVariable("anything"));
    assertFalse(detector.isVariable("123"));
  }

  @Test
  public void testNullInput() {
    Pattern pattern = Pattern.compile("\\d+");
    Set<Pattern> patterns = new HashSet<>();
    patterns.add(pattern);
    VariableDetector detector = new CustomVariableDetector(patterns, new HashSet<>(), false);

    assertFalse(detector.isVariable(null));
  }
}
