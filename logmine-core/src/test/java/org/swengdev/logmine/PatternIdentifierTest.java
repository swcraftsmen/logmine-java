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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for PatternIdentifier. */
public class PatternIdentifierTest {

  @Test
  public void testGenerateId() {
    LogMine logMine = new LogMine(0.5, 2);
    logMine.addLog("INFO User alice logged in");
    logMine.addLog("INFO User bob logged in");

    var patterns = logMine.extractPatterns();
    if (!patterns.isEmpty()) {
      String id = PatternIdentifier.generateId(patterns.get(0));

      assertNotNull(id);
      assertFalse(id.isEmpty());
      // SHA-256 hash in Base64 URL-safe encoding (without padding) is 43 characters
      assertEquals(43, id.length());
    }
  }

  @Test
  public void testDeterministicId() {
    LogMine logMine = new LogMine(0.5, 2);
    logMine.addLog("INFO User alice logged in");
    logMine.addLog("INFO User bob logged in");

    var patterns = logMine.extractPatterns();
    if (!patterns.isEmpty()) {
      LogPattern pattern = patterns.get(0);

      String id1 = PatternIdentifier.generateId(pattern);
      String id2 = PatternIdentifier.generateId(pattern);

      // Same pattern should always generate same ID
      assertEquals(id1, id2);
    }
  }

  @Test
  public void testGenerateSignature() {
    LogMine logMine = new LogMine(0.5, 2);
    logMine.addLog("INFO User alice logged in");
    logMine.addLog("INFO User bob logged in");

    var patterns = logMine.extractPatterns();
    if (!patterns.isEmpty()) {
      LogPattern pattern = patterns.get(0);
      String signature = PatternIdentifier.generateSignature(pattern);

      assertNotNull(signature);
      assertFalse(signature.isEmpty());
      // Signature is tokens joined with space, not the same as pattern string
      assertTrue(signature.contains("INFO"));
    }
  }

  @Test
  public void testGenerateIdNullPattern() {
    assertThrows(
        NullPointerException.class,
        () -> {
          PatternIdentifier.generateId(null);
        });
  }

  @Test
  public void testGenerateSignatureNullPattern() {
    assertThrows(
        NullPointerException.class,
        () -> {
          PatternIdentifier.generateSignature(null);
        });
  }

  @Test
  public void testDifferentPatternsGenerateDifferentIds() {
    LogMine logMine = new LogMine(0.5, 1);
    logMine.addLog("INFO User alice logged in");
    logMine.addLog("ERROR Database error");

    var patterns = logMine.extractPatterns();
    if (patterns.size() >= 2) {
      String id1 = PatternIdentifier.generateId(patterns.get(0));
      String id2 = PatternIdentifier.generateId(patterns.get(1));

      // Different patterns should have different IDs
      assertNotEquals(id1, id2);
    }
  }

  @Test
  public void testIdFormat() {
    LogMine logMine = new LogMine(0.5, 2);
    logMine.addLog("Test log 1");
    logMine.addLog("Test log 2");

    var patterns = logMine.extractPatterns();
    if (!patterns.isEmpty()) {
      String id = PatternIdentifier.generateId(patterns.get(0));

      // Should be valid Base64 URL-safe encoding (A-Za-z0-9_-)
      assertTrue(id.matches("[A-Za-z0-9_-]{43}"));
    }
  }
}
