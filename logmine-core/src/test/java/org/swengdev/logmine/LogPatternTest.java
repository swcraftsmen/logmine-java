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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.swengdev.logmine.strategy.StandardVariableDetector;
import org.swengdev.logmine.strategy.WhitespaceTokenizer;

/** Tests for LogPattern. */
public class LogPatternTest {

  private final StandardVariableDetector detector = new StandardVariableDetector();
  private final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();

  @Test
  public void testCreateFromMessages() {
    List<LogMessage> messages =
        Arrays.asList(
            new LogMessage(
                "INFO User alice logged in",
                tokenizer.tokenize("INFO User alice logged in"),
                detector),
            new LogMessage(
                "INFO User bob logged in",
                tokenizer.tokenize("INFO User bob logged in"),
                detector));

    LogPattern pattern = LogPattern.createFromMessages(messages, detector);

    assertNotNull(pattern);
    assertEquals(2, pattern.getSupportCount());
    assertTrue(pattern.getPatternString().contains("***")); // Should have wildcard for name
  }

  @Test
  public void testMatches() {
    List<LogMessage> messages =
        Arrays.asList(
            new LogMessage(
                "INFO User alice logged in",
                tokenizer.tokenize("INFO User alice logged in"),
                detector),
            new LogMessage(
                "INFO User bob logged in",
                tokenizer.tokenize("INFO User bob logged in"),
                detector));

    LogPattern pattern = LogPattern.createFromMessages(messages, detector);

    LogMessage newMessage =
        new LogMessage(
            "INFO User charlie logged in",
            tokenizer.tokenize("INFO User charlie logged in"),
            detector);

    assertTrue(pattern.matches(newMessage));
  }

  @Test
  public void testDoesNotMatch() {
    List<LogMessage> messages =
        Arrays.asList(
            new LogMessage(
                "INFO User alice logged in",
                tokenizer.tokenize("INFO User alice logged in"),
                detector),
            new LogMessage(
                "INFO User bob logged in",
                tokenizer.tokenize("INFO User bob logged in"),
                detector));

    LogPattern pattern = LogPattern.createFromMessages(messages, detector);

    LogMessage differentMessage =
        new LogMessage(
            "ERROR Database failed", tokenizer.tokenize("ERROR Database failed"), detector);

    assertFalse(pattern.matches(differentMessage));
  }

  @Test
  public void testSpecificity() {
    // Pattern with all constants
    List<LogMessage> constantMessages =
        Arrays.asList(
            new LogMessage(
                "ERROR Database failed", tokenizer.tokenize("ERROR Database failed"), detector),
            new LogMessage(
                "ERROR Database failed", tokenizer.tokenize("ERROR Database failed"), detector));

    LogPattern constantPattern = LogPattern.createFromMessages(constantMessages, detector);

    // Pattern with some variables
    List<LogMessage> variableMessages =
        Arrays.asList(
            new LogMessage(
                "INFO User alice logged in",
                tokenizer.tokenize("INFO User alice logged in"),
                detector),
            new LogMessage(
                "INFO User bob logged in",
                tokenizer.tokenize("INFO User bob logged in"),
                detector));

    LogPattern variablePattern = LogPattern.createFromMessages(variableMessages, detector);

    // Constant pattern should have higher specificity
    assertTrue(constantPattern.getSpecificity() >= variablePattern.getSpecificity());
  }

  @Test
  public void testGetPatternString() {
    List<String> tokens = Arrays.asList("INFO", "***", "logged", "in");
    LogPattern pattern = new LogPattern(tokens, 5, detector);

    assertEquals("INFO***loggedin", pattern.getPatternString());
  }

  @Test
  public void testGetSupportCount() {
    List<String> tokens = Arrays.asList("INFO", "***");
    LogPattern pattern = new LogPattern(tokens, 10, detector);

    assertEquals(10, pattern.getSupportCount());
  }

  @Test
  public void testGetPatternTokens() {
    List<String> tokens = Arrays.asList("INFO", "***", "logged");
    LogPattern pattern = new LogPattern(tokens, 5, detector);

    List<String> retrievedTokens = pattern.getPatternTokens();

    assertEquals(3, retrievedTokens.size());
    assertEquals("INFO", retrievedTokens.get(0));
    assertEquals("***", retrievedTokens.get(1));
    assertEquals("logged", retrievedTokens.get(2));
  }

  @Test
  public void testEmptyPattern() {
    LogPattern pattern = LogPattern.createFromMessages(Arrays.asList(), detector);

    assertEquals(0, pattern.getSupportCount());
    assertEquals(0.0, pattern.getSpecificity(), 0.01);
  }

  @Test
  public void testSingleMessage() {
    List<LogMessage> messages =
        Arrays.asList(
            new LogMessage(
                "INFO Single message", tokenizer.tokenize("INFO Single message"), detector));

    LogPattern pattern = LogPattern.createFromMessages(messages, detector);

    assertEquals(1, pattern.getSupportCount());
  }

  @Test
  public void testEquals() {
    List<String> tokens = Arrays.asList("INFO", "***");
    LogPattern pattern1 = new LogPattern(tokens, 5, detector);
    LogPattern pattern2 = new LogPattern(tokens, 10, detector); // Different support count

    // Should be equal if pattern string is same (support count doesn't matter)
    assertEquals(pattern1, pattern2);
  }

  @Test
  public void testHashCode() {
    List<String> tokens = Arrays.asList("INFO", "***");
    LogPattern pattern1 = new LogPattern(tokens, 5, detector);
    LogPattern pattern2 = new LogPattern(tokens, 5, detector);

    assertEquals(pattern1.hashCode(), pattern2.hashCode());
  }

  @Test
  public void testToString() {
    List<String> tokens = Arrays.asList("INFO", "***");
    LogPattern pattern = new LogPattern(tokens, 5, detector);

    String str = pattern.toString();

    assertTrue(str.contains("INFO"));
    assertTrue(str.contains("5"));
  }

  @Test
  public void testMatchesDifferentLength() {
    List<LogMessage> messages =
        Arrays.asList(
            new LogMessage(
                "INFO User logged in", tokenizer.tokenize("INFO User logged in"), detector),
            new LogMessage(
                "INFO User logged in", tokenizer.tokenize("INFO User logged in"), detector));

    LogPattern pattern = LogPattern.createFromMessages(messages, detector);

    LogMessage shorterMessage =
        new LogMessage("INFO User", tokenizer.tokenize("INFO User"), detector);

    assertFalse(pattern.matches(shorterMessage));
  }
}
