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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests to verify LogMineConfig works correctly as a record. */
public class LogMineConfigRecordTest {

  @Test
  public void testRecordIsImmutable() {
    LogMineConfig config = LogMineConfig.defaults();

    // Records are immutable - no setters should exist
    // This test verifies the record has proper getters
    assertNotNull(config.tokenizerStrategy());
    assertNotNull(config.variableDetector());
    assertEquals(0.5, config.similarityThreshold(), 0.01);
  }

  @Test
  public void testRecordEquality() {
    LogMineConfig config1 = LogMineConfig.defaults();
    LogMineConfig config2 = LogMineConfig.defaults();

    // Different builders create different object instances for strategies,
    // so even with same values, they won't be equal unless strategies implement equals
    // This test just verifies records work correctly
    assertNotNull(config1);
    assertNotNull(config2);

    // Same instance should equal itself
    assertEquals(config1, config1);
  }

  @Test
  public void testRecordHashCode() {
    // Same config instance should always have same hashCode
    LogMineConfig config = LogMineConfig.defaults();

    int hash1 = config.hashCode();
    int hash2 = config.hashCode();

    // Records automatically implement hashCode() - should be consistent
    assertEquals(hash1, hash2);

    // Also test that records generate a hashCode
    assertNotEquals(0, config.hashCode());
  }

  @Test
  public void testRecordEqualityWithSameComponents() {
    // Create two configs using exact same strategy instances
    var tokenizer = new org.swengdev.logmine.strategy.WhitespaceTokenizer();
    var detector = new org.swengdev.logmine.strategy.StandardVariableDetector();

    LogMineConfig config1 =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.7)
            .withMinClusterSize(3)
            .withTokenizerStrategy(tokenizer)
            .withVariableDetector(detector)
            .build();

    LogMineConfig config2 =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.7)
            .withMinClusterSize(3)
            .withTokenizerStrategy(tokenizer) // Same instance
            .withVariableDetector(detector) // Same instance
            .build();

    // Records with same component references should be equal
    assertEquals(config1, config2);
    assertEquals(config1.hashCode(), config2.hashCode());
  }

  @Test
  public void testRecordToString() {
    LogMineConfig config = LogMineConfig.defaults();

    String str = config.toString();

    // Records automatically generate toString()
    assertNotNull(str);
    assertTrue(str.contains("LogMineConfig"));
    assertTrue(str.contains("similarityThreshold"));
  }

  @Test
  public void testCollectionsAreImmutable() {
    LogMineConfig config =
        LogMineConfig.builder().ignoreToken("test").addHierarchyThreshold(0.5).build();

    // Should throw UnsupportedOperationException
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          config.ignoreTokens().add("another");
        });

    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          config.hierarchyThresholds().add(0.7);
        });
  }
}
