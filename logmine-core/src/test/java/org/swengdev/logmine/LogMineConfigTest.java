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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.swengdev.logmine.strategy.RegexTokenizer;
import org.swengdev.logmine.strategy.StandardVariableDetector;
import org.swengdev.logmine.strategy.TokenizerStrategy;
import org.swengdev.logmine.strategy.VariableDetector;
import org.swengdev.logmine.strategy.WhitespaceTokenizer;

/** Tests for LogMineConfig. */
public class LogMineConfigTest {

  @Test
  public void testDefaultConfig() {
    LogMineConfig config = LogMineConfig.defaults();

    assertNotNull(config);
    assertEquals(0.5, config.similarityThreshold(), 0.01);
    assertEquals(1, config.minClusterSize());
    assertNotNull(config.tokenizerStrategy());
    assertNotNull(config.variableDetector());
  }

  @Test
  public void testBuilderWithSimilarityThreshold() {
    LogMineConfig config = LogMineConfig.builder().withSimilarityThreshold(0.7).build();

    assertEquals(0.7, config.similarityThreshold(), 0.01);
  }

  @Test
  public void testBuilderWithMinClusterSize() {
    LogMineConfig config = LogMineConfig.builder().withMinClusterSize(5).build();

    assertEquals(5, config.minClusterSize());
  }

  @Test
  public void testBuilderWithTokenizer() {
    TokenizerStrategy tokenizer = new RegexTokenizer("\\s+");

    LogMineConfig config = LogMineConfig.builder().withTokenizerStrategy(tokenizer).build();

    assertEquals(tokenizer, config.tokenizerStrategy());
  }

  @Test
  public void testBuilderWithVariableDetector() {
    VariableDetector detector = new StandardVariableDetector();

    LogMineConfig config = LogMineConfig.builder().withVariableDetector(detector).build();

    assertEquals(detector, config.variableDetector());
  }

  @Test
  public void testBuilderWithAllOptions() {
    TokenizerStrategy tokenizer = new WhitespaceTokenizer();
    VariableDetector detector = new StandardVariableDetector();

    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .withMinClusterSize(3)
            .withTokenizerStrategy(tokenizer)
            .withVariableDetector(detector)
            .build();

    assertEquals(0.6, config.similarityThreshold(), 0.01);
    assertEquals(3, config.minClusterSize());
    assertEquals(tokenizer, config.tokenizerStrategy());
    assertEquals(detector, config.variableDetector());
  }

  @Test
  public void testInvalidSimilarityThreshold() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LogMineConfig.builder().withSimilarityThreshold(-0.1).build();
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LogMineConfig.builder().withSimilarityThreshold(1.5).build();
        });
  }

  @Test
  public void testInvalidMinClusterSize() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LogMineConfig.builder().withMinClusterSize(0).build();
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LogMineConfig.builder().withMinClusterSize(-1).build();
        });
  }

  @Test
  public void testNullTokenizer() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LogMineConfig.builder().withTokenizerStrategy(null).build();
        });
  }

  @Test
  public void testNullVariableDetector() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LogMineConfig.builder().withVariableDetector(null).build();
        });
  }

  @Test
  public void testBuilderChaining() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.7)
            .withMinClusterSize(4)
            .withTokenizerStrategy(new WhitespaceTokenizer())
            .withVariableDetector(new StandardVariableDetector())
            .build();

    assertNotNull(config);
  }

  @Test
  public void testToString() {
    LogMineConfig config = LogMineConfig.defaults();
    String str = config.toString();

    assertNotNull(str);
    assertTrue(str.contains("LogMineConfig"));
    assertTrue(str.contains("similarityThreshold"));
  }
}
