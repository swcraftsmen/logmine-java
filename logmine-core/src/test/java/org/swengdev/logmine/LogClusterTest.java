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
import org.swengdev.logmine.strategy.StandardVariableDetector;
import org.swengdev.logmine.strategy.WhitespaceTokenizer;

/** Tests for LogCluster. */
public class LogClusterTest {

  private final StandardVariableDetector detector = new StandardVariableDetector();
  private final WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();

  @Test
  public void testAddSimilarMessage() {
    LogMessage msg1 =
        new LogMessage(
            "INFO User alice logged in", tokenizer.tokenize("INFO User alice logged in"), detector);
    LogMessage msg2 =
        new LogMessage(
            "INFO User bob logged in", tokenizer.tokenize("INFO User bob logged in"), detector);

    LogCluster cluster = new LogCluster(msg1, detector);
    boolean added = cluster.addMessage(msg2, 0.5);

    assertTrue(added);
    assertEquals(2, cluster.size());
  }

  @Test
  public void testRejectDissimilarMessage() {
    LogMessage msg1 =
        new LogMessage(
            "INFO User alice logged in", tokenizer.tokenize("INFO User alice logged in"), detector);
    LogMessage msg2 =
        new LogMessage(
            "ERROR Database failed completely",
            tokenizer.tokenize("ERROR Database failed completely"),
            detector);

    LogCluster cluster = new LogCluster(msg1, detector);
    boolean added = cluster.addMessage(msg2, 0.8); // High threshold

    assertFalse(added);
    assertEquals(1, cluster.size());
  }

  @Test
  public void testGeneratePattern() {
    LogMessage msg1 =
        new LogMessage(
            "INFO User alice logged in", tokenizer.tokenize("INFO User alice logged in"), detector);
    LogMessage msg2 =
        new LogMessage(
            "INFO User bob logged in", tokenizer.tokenize("INFO User bob logged in"), detector);

    LogCluster cluster = new LogCluster(msg1, detector);
    cluster.addMessage(msg2, 0.5);

    LogPattern pattern = cluster.generatePattern();

    assertNotNull(pattern);
    assertEquals(2, pattern.getSupportCount());
  }

  @Test
  public void testGetPattern() {
    LogMessage msg = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);

    LogCluster cluster = new LogCluster(msg, detector);
    LogPattern pattern = cluster.getPattern();

    assertNotNull(pattern);
  }

  @Test
  public void testGetCentroid() {
    LogMessage msg = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);

    LogCluster cluster = new LogCluster(msg, detector);
    LogMessage centroid = cluster.getCentroid();

    assertNotNull(centroid);
    assertEquals(msg.getRawMessage(), centroid.getRawMessage());
  }

  @Test
  public void testGetMessages() {
    LogMessage msg1 = new LogMessage("INFO Test 1", tokenizer.tokenize("INFO Test 1"), detector);
    LogMessage msg2 = new LogMessage("INFO Test 2", tokenizer.tokenize("INFO Test 2"), detector);

    LogCluster cluster = new LogCluster(msg1, detector);
    cluster.addMessage(msg2, 0.5);

    assertEquals(2, cluster.getMessages().size());
  }

  @Test
  public void testSize() {
    LogMessage msg = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);

    LogCluster cluster = new LogCluster(msg, detector);

    assertEquals(1, cluster.size());
  }

  @Test
  public void testToString() {
    LogMessage msg = new LogMessage("INFO Test", tokenizer.tokenize("INFO Test"), detector);

    LogCluster cluster = new LogCluster(msg, detector);
    String str = cluster.toString();

    assertNotNull(str);
    assertTrue(str.contains("Cluster"));
    assertTrue(str.contains("size=1"));
  }

  @Test
  public void testMultipleAdditions() {
    LogMessage msg1 =
        new LogMessage(
            "INFO User alice logged in", tokenizer.tokenize("INFO User alice logged in"), detector);

    LogCluster cluster = new LogCluster(msg1, detector);

    for (int i = 0; i < 10; i++) {
      LogMessage msg =
          new LogMessage(
              "INFO User user" + i + " logged in",
              tokenizer.tokenize("INFO User user" + i + " logged in"),
              detector);
      cluster.addMessage(msg, 0.5);
    }

    assertTrue(cluster.size() > 1);
  }
}
