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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.swengdev.logmine.strategy.StandardVariableDetector;
import org.swengdev.logmine.strategy.WhitespaceTokenizer;

/** Tests for LogMessage. */
public class LogMessageTest {

  private final StandardVariableDetector detector = new StandardVariableDetector();
  private final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();

  @Test
  public void testBasicConstruction() {
    String raw = "INFO User logged in";
    LogMessage message = new LogMessage(raw, tokenizer.tokenize(raw), detector);

    assertEquals(raw, message.getRawMessage());
    assertEquals(4, message.getLength());
    assertEquals(4, message.getTokens().size());
  }

  @Test
  public void testSimilarityIdentical() {
    LogMessage msg1 = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);
    LogMessage msg2 = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);

    double similarity = msg1.similarity(msg2);

    assertEquals(1.0, similarity, 0.01);
  }

  @Test
  public void testSimilaritySimilar() {
    LogMessage msg1 =
        new LogMessage(
            "INFO User alice logged in", tokenizer.tokenize("INFO User alice logged in"), detector);
    LogMessage msg2 =
        new LogMessage(
            "INFO User bob logged in", tokenizer.tokenize("INFO User bob logged in"), detector);

    double similarity = msg1.similarity(msg2);

    assertTrue(similarity > 0.5); // Should be fairly similar
  }

  @Test
  public void testSimilarityDifferent() {
    LogMessage msg1 =
        new LogMessage("INFO User logged in", tokenizer.tokenize("INFO User logged in"), detector);
    LogMessage msg2 =
        new LogMessage(
            "ERROR Database failed", tokenizer.tokenize("ERROR Database failed"), detector);

    double similarity = msg1.similarity(msg2);

    assertTrue(similarity < 0.5); // Should be quite different
  }

  @Test
  public void testEditDistance() {
    LogMessage msg1 = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);
    LogMessage msg2 =
        new LogMessage("INFO Test Message", tokenizer.tokenize("INFO Test Message"), detector);

    int distance = msg1.editDistance(msg2);

    // Should have distance of 1 (one token added)
    assertEquals(1, distance);
  }

  @Test
  public void testEditDistanceIdentical() {
    LogMessage msg1 = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);
    LogMessage msg2 = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);

    int distance = msg1.editDistance(msg2);

    assertEquals(0, distance);
  }

  @Test
  public void testGetRawMessage() {
    String raw = "INFO User logged in";
    LogMessage message = new LogMessage(raw, tokenizer.tokenize(raw), detector);

    assertEquals(raw, message.getRawMessage());
  }

  @Test
  public void testGetProcessedMessage() {
    String raw = "INFO User logged in";
    String processed = "info user logged in";
    LogMessage message = new LogMessage(raw, processed, tokenizer.tokenize(raw), detector);

    assertEquals(processed, message.getProcessedMessage());
  }

  @Test
  public void testGetTokens() {
    String raw = "INFO User logged";
    LogMessage message = new LogMessage(raw, tokenizer.tokenize(raw), detector);

    var tokens = message.getTokens();

    assertEquals(3, tokens.size());
    assertEquals("INFO", tokens.get(0));
    assertEquals("User", tokens.get(1));
    assertEquals("logged", tokens.get(2));
  }

  @Test
  public void testGetLength() {
    String raw = "One Two Three Four";
    LogMessage message = new LogMessage(raw, tokenizer.tokenize(raw), detector);

    assertEquals(4, message.getLength());
  }

  @Test
  public void testToString() {
    String raw = "INFO Test Message";
    LogMessage message = new LogMessage(raw, tokenizer.tokenize(raw), detector);

    assertEquals(raw, message.toString());
  }

  @Test
  public void testEmptyMessage() {
    LogMessage message = new LogMessage("", Arrays.asList(), detector);

    assertEquals(0, message.getLength());
    assertEquals(1.0, message.similarity(message), 0.01);
  }

  @Test
  public void testSimilarityWithEmptyMessage() {
    LogMessage msg1 = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);
    LogMessage msg2 = new LogMessage("", Arrays.asList(), detector);

    double similarity = msg1.similarity(msg2);

    assertTrue(similarity < 1.0);
  }
}
