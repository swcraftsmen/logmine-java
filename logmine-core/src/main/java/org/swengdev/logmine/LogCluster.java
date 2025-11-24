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

import java.util.ArrayList;
import java.util.List;
import org.swengdev.logmine.strategy.VariableDetector;

/**
 * Represents a cluster of similar log messages. Each cluster will be used to generate a log
 * pattern.
 *
 * <p><b>Internal API:</b> This class is package-private and not intended for direct use by library
 * users. Clustering is an implementation detail. Users should work with {@link LogPattern} objects
 * which represent the final extracted patterns.
 */
class LogCluster {
  private final List<LogMessage> messages;
  private LogMessage representative;
  private LogPattern pattern;
  private final VariableDetector variableDetector;

  /**
   * Creates a new cluster with the first log message.
   *
   * @param firstMessage The initial message for this cluster
   * @param variableDetector Strategy for detecting variable parts in tokens
   */
  public LogCluster(LogMessage firstMessage, VariableDetector variableDetector) {
    this.messages = new ArrayList<>();
    this.messages.add(firstMessage);
    this.representative = firstMessage;
    this.variableDetector = variableDetector;
  }

  /**
   * Adds a log message to this cluster if it's similar enough. Returns true if the message was
   * added.
   *
   * @param message The message to potentially add to this cluster
   * @param threshold Similarity threshold (0.0-1.0) for cluster membership
   * @return true if the message was added, false otherwise
   */
  public boolean addMessage(LogMessage message, double threshold) {
    double similarity = representative.similarity(message);

    if (similarity >= threshold) {
      messages.add(message);
      updateRepresentative();
      return true;
    }

    return false;
  }

  /**
   * Updates the representative message (currently using the first message). Could be enhanced to
   * use a centroid or medoid.
   */
  private void updateRepresentative() {
    // For simplicity, keep the first message as representative
    // In a more sophisticated implementation, we could compute a centroid
    representative = messages.getFirst();
  }

  /**
   * Generates a pattern from all messages in this cluster.
   *
   * @return The generated log pattern for this cluster
   */
  public LogPattern generatePattern() {
    if (pattern == null) {
      pattern = LogPattern.createFromMessages(messages, variableDetector);
    }
    return pattern;
  }

  /**
   * Gets the centroid message of this cluster. Currently returns the representative message.
   *
   * @return The representative message for this cluster
   */
  public LogMessage getCentroid() {
    return representative;
  }

  /**
   * Gets all messages in this cluster.
   *
   * @return A defensive copy of the message list
   */
  public List<LogMessage> getMessages() {
    return new ArrayList<>(messages);
  }

  /**
   * Gets the number of messages in this cluster.
   *
   * @return The cluster size
   */
  public int size() {
    return messages.size();
  }

  /**
   * Gets the pattern for this cluster, generating it if necessary.
   *
   * @return The log pattern for this cluster
   */
  public LogPattern getPattern() {
    if (pattern == null) {
      generatePattern();
    }
    return pattern;
  }

  /**
   * Calculates the similarity between a message and this cluster's representative. Used for finding
   * the closest cluster when max cluster limit is reached.
   *
   * @param message Message to compare
   * @return Similarity score between 0.0 and 1.0
   */
  public double calculateSimilarity(LogMessage message) {
    return representative.similarity(message);
  }

  @Override
  public String toString() {
    return "Cluster(size="
        + messages.size()
        + ", pattern="
        + (pattern != null ? pattern.getPatternString() : "not generated")
        + ")";
  }
}
